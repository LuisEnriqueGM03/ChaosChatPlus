"""Launch exported game commands and verify real multiplayer outcomes, without external accounts."""
from pathlib import Path
import argparse
import json
import os
import shutil
import subprocess
import time

ROOT = Path(__file__).resolve().parent.parent

def verify(home, loader):
    signals=home/'signals'
    if list(signals.glob('*.FAIL')):raise RuntimeError('Runtime failure signal present')
    server_log=(home/'server/logs/latest.log').read_text(encoding='utf-8',errors='replace')
    if 'All dimensions are saved' not in server_log:raise RuntimeError('No clean server save confirmation')
    reports=(home/'server/config/chaoschatplus/reports.log').read_text(encoding='utf-8')
    if 'REPORTE de Pepe' not in reports or 'REPORTE de Audit' in reports:raise RuntimeError('Report access check failed')
    checks=[]
    for role in ['Luis','Pepe','Audit']:
        signal=(signals/f'{role}.done').read_text()
        log=(home/role/'logs/latest.log').read_text(encoding='utf-8',errors='replace')
        lines=[line for line in log.splitlines() if '[RuntimeTest] PASS '+role+':' in line]
        if signal != f'PASS {len(lines)}':raise RuntimeError('Client completion does not match assertions: '+role)
        checks.extend(lines)
    result={'loader':loader,'result':'PASS','checks':len(checks),'players':['Luis','Pepe','Audit'],
        'dedicatedServer':'clean startup and save','privateAccess':'third-party screenshot request and report denied',
        'assertions':checks}
    (home/'RESULT.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
    print(f'PASS {loader}: {len(checks)} runtime assertions, three real clients, clean dedicated server shutdown.',flush=True)

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('loader',choices=['fabric','forge','neoforge'])
    parser.add_argument('--verify-existing',action='store_true')
    args=parser.parse_args()
    home=ROOT/'.verification/runtime'/args.loader
    if args.verify_existing:return verify(home,args.loader)
    signals=home/'signals'
    signals.mkdir(exist_ok=True)
    config=home/'server/config/chaoschatplus'
    if config.exists():
        # Preserve previous test configuration while ensuring uploads and reloads start from a fresh registry.
        config.rename(config.with_name('chaoschatplus-previous-'+str(time.time_ns())))
    if list(signals.iterdir()):
        archive=home/f'signals-{int(time.time())}'
        signals.rename(archive)
        signals.mkdir()
    if args.loader != 'fabric':
        # Avoid concurrent Forge splash windows competing for the test GPU.
        for role in ['Luis','Pepe','Audit']:
            config_file=home/role/'config/fml.toml'
            config_file.parent.mkdir(parents=True,exist_ok=True)
            text=config_file.read_text() if config_file.exists() else ''
            if 'earlyWindowControl = true' in text:text=text.replace('earlyWindowControl = true','earlyWindowControl = false')
            elif 'earlyWindowControl' not in text:text+='\nearlyWindowControl = false\n'
            config_file.write_text(text)
    processes=[];streams=[];launch_times={}
    def launch(role):
        config=json.loads((home/'commands'/f'{role}.json').read_text())
        env=os.environ.copy();env.update(config['environment'])
        stream=(home/f'{role}-console.log').open('wb');streams.append(stream)
        launch_times[role]=time.time_ns()
        process=subprocess.Popen([config['command'][0],'-XX:ActiveProcessorCount=2',*config['command'][1:]],cwd=config['cwd'],env=env,
            stdin=subprocess.PIPE,stdout=stream,stderr=subprocess.STDOUT,
            creationflags=subprocess.CREATE_NO_WINDOW)
        processes.append((role,process))
        print(f'Started {args.loader} {role}, PID {process.pid}',flush=True)
        return process
    def game_log(role):
        p=home/role/'logs/latest.log'
        return p.read_text(encoding='utf-8',errors='replace') if p.exists() and p.stat().st_mtime_ns >= launch_times.get(role,0) else ''
    server=None
    try:
        server=launch('server')
        start=time.monotonic()
        while 'Done (' not in game_log('server'):
            if server.poll() is not None:raise RuntimeError('Dedicated server exited before readiness')
            if time.monotonic()-start>240:raise RuntimeError('Dedicated server startup timed out')
            time.sleep(1)
        for command in ['op Luis','gamerule minecraft:advance_time false','time set day','gamerule minecraft:spawn_mobs false']:
            server.stdin.write((command+'\n').encode());server.stdin.flush()
        # Establish each connection before starting the next graphical process.
        # This avoids status/login timeouts while three clients compile shaders concurrently.
        for role in ['Luis','Pepe','Audit']:
            client=launch(role)
            connect_start=time.monotonic()
            while f'{role}[' not in game_log('server'):
                if client.poll() is not None:raise RuntimeError(role+' exited before joining')
                if list(signals.glob('*.FAIL')):raise RuntimeError('Client failed during startup')
                if time.monotonic()-connect_start>210:raise RuntimeError(role+' initial connection timed out')
                time.sleep(1)
        start=time.monotonic();last_status=0;hot_added=False;online_op=False
        while True:
            failures=list(signals.glob('*.FAIL'))
            if failures:raise RuntimeError('; '.join(p.name+': '+p.read_text() for p in failures))
            if (signals/'Luis.ready').exists() and not online_op:
                server.stdin.write(b'op Luis\n');server.stdin.flush();online_op=True
            if (signals/'Luis.reload').exists() and not hot_added:
                target=home/'server/config/chaoschatplus/emojis/runtime/runtime_added.png'
                target.parent.mkdir(parents=True,exist_ok=True)
                shutil.copy2(ROOT/'src/main/resources/assets/chaoschatplus/textures/emoji/fuego.png',target)
                hot_added=True
                print('Added custom PNG to server directory; waiting for automatic synchronization.',flush=True)
            done=[role for role in ['Luis','Pepe','Audit'] if (signals/f'{role}.done').exists()]
            if len(done)==3:break
            for role,process in processes:
                if process.poll() is not None and role not in done:
                    raise RuntimeError(f'{role} exited before test completion, exit {process.returncode}')
            if time.monotonic()-start>650:raise RuntimeError('Multiplayer test timed out')
            if time.monotonic()-last_status>30:
                print('Progress:',', '.join(p.name for p in signals.iterdir()),flush=True)
                last_status=time.monotonic()
            time.sleep(1)
        for role,process in processes:
            if role!='server':process.wait(timeout=45)
        server.stdin.write(b'stop\n');server.stdin.flush();server.wait(timeout=60)
        log=game_log('server')
        if 'All dimensions are saved' not in log and 'All chunks are saved' not in log:
            raise RuntimeError('No clean server world-save confirmation')
        reports=home/'server/config/chaoschatplus/reports.log'
        if not reports.exists() or 'REPORTE de Pepe' not in reports.read_text(encoding='utf-8'):
            raise RuntimeError('Recipient screenshot report not recorded')
        if 'REPORTE de Audit' in reports.read_text(encoding='utf-8'):
            raise RuntimeError('Unauthorized report from third player was accepted')
        verify(home,args.loader)
    finally:
        for role,process in processes:
            if process.poll() is None:
                if role=='server':
                    try:process.stdin.write(b'stop\n');process.stdin.flush();process.wait(timeout=30)
                    except (OSError,subprocess.TimeoutExpired):process.terminate()
                else:process.terminate()
        for stream in streams:stream.close()

if __name__=='__main__':main()
