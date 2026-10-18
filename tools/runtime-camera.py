"""Targeted camera regression using an exported dedicated server and graphical client."""
from pathlib import Path
import argparse
import json
import os
import shutil
import subprocess
import time
from runtime_evidence import clean_save

ROOT = Path(__file__).resolve().parent.parent

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('loader', choices=['fabric', 'forge', 'neoforge'])
    args = parser.parse_args()
    home = ROOT / '.verification/runtime' / args.loader
    evidence = home / 'multiplayer-evidence'
    evidence.mkdir(exist_ok=True)
    for role in ['Luis', 'Pepe', 'Audit', 'server']:
        log = home / role / 'logs/latest.log'
        target = evidence / (role + '.log')
        if log.exists() and not target.exists():
            shutil.copy2(log, target)
    signal = home / 'signals/Luis.camera'
    signal.unlink(missing_ok=True)
    processes = []
    streams = []
    def launch(role):
        config = json.loads((home / 'commands' / (role + '.json')).read_text())
        command = config['command'].copy()
        if role == 'Luis':
            original = Path(command[1][1:])
            argfile = home / 'commands/camera-args.txt'
            argfile.write_text('"-Dchaoschatplus.cameraTest=true"\n' + original.read_text())
            command[1] = '@' + str(argfile)
        stream = (home / (role + '-camera-console.log')).open('wb')
        streams.append(stream)
        env = os.environ.copy()
        env.update(config['environment'])
        process = subprocess.Popen([command[0], '-XX:ActiveProcessorCount=2', *command[1:]],
            cwd=config['cwd'], env=env, stdin=subprocess.PIPE, stdout=stream,
            stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
        processes.append(process)
        return process
    try:
        started = time.time_ns()
        server = launch('server')
        log = home / 'server/logs/latest.log'
        deadline = time.monotonic() + 240
        while not (log.exists() and log.stat().st_mtime_ns >= started and 'Done (' in log.read_text(errors='replace')):
            if server.poll() is not None or time.monotonic() > deadline:
                raise RuntimeError('Camera server failed to start')
            time.sleep(1)
        client = launch('Luis')
        client.wait(timeout=240)
        if client.returncode != 0 or not signal.exists() or signal.read_text() != 'PASS 3':
            raise RuntimeError('Camera regression failed; inspect client camera log')
        server.stdin.write(b'stop\n')
        server.stdin.flush()
        server.wait(timeout=60)
        if server.returncode != 0 or not clean_save(home):
            raise RuntimeError('Camera server did not save cleanly')
        result = {'loader': args.loader, 'result': 'PASS', 'checks': 3,
            'coverage': ['actual projection field of view and camera zoom', 'HUD suppression render hook', 'preview and restoration'],
            'screenshots': ['runtime_camera.png', 'runtime_preview.png']}
        (home / 'CAMERA_RESULT.json').write_text(json.dumps(result, indent=2) + '\n')
        print('PASS', args.loader, 'three camera regressions; inspect camera and preview screenshots.')
    finally:
        for process in processes:
            if process.poll() is None:
                process.terminate()
        for stream in streams:
            stream.close()

if __name__ == '__main__':
    main()
