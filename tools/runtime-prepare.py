"""Prepare isolated Windows development launches for runtime-tests.py (JDK 25 required)."""
from pathlib import Path
import argparse
import copy
import json
import os
import subprocess

ROOT=Path(__file__).resolve().parent.parent

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('loader',choices=['fabric','forge','neoforge'])
    parser.add_argument('--jdk21',type=Path,help='Deprecated; Java 25 is used for all tools')
    parser.add_argument('--jdk25',type=Path,required=True)
    args=parser.parse_args()
    home=ROOT/'.verification/runtime'/args.loader
    server=home/'server';server.mkdir(parents=True,exist_ok=True)
    port={'fabric':26031,'forge':26032,'neoforge':26033}[args.loader]
    (server/'eula.txt').write_text('eula=true\n')
    (server/'server.properties').write_text(f'server-ip=127.0.0.1\nserver-port={port}\nonline-mode=false\nenforce-secure-profile=false\nlevel-type=minecraft:flat\ngenerate-structures=false\nspawn-protection=0\nview-distance=2\nsimulation-distance=2\nmax-players=5\ngamemode=creative\nspawn-monsters=false\nspawn-animals=false\nspawn-npcs=false\n')
    for role in ['Luis','Pepe','Audit']:
        game=home/role;game.mkdir(parents=True,exist_ok=True)
        (game/'options.txt').write_text('version:4903\nrenderDistance:2\nsimulationDistance:5\nmaxFps:30\nguiScale:2\npauseOnLostFocus:false\nlang:es_es\nonboardAccessibility:false\nskipMultiplayerWarning:true\ntutorialStep:none\nparticles:2\nentityDistanceScaling:0.5\n')
    classes=ROOT/'.verification/capture-classes';classes.mkdir(parents=True,exist_ok=True)
    manifest=ROOT/'.verification/capture-manifest.mf';manifest.write_text('Premain-Class: RuntimeCapture\n')
    subprocess.run([str(args.jdk25/'bin/javac.exe'),'-d',str(classes),str(ROOT/'tools/RuntimeCapture.java')],check=True)
    subprocess.run([str(args.jdk25/'bin/jar.exe'),'cfm',str(ROOT/'.verification/runtime-capture.jar'),str(manifest),'-C',str(classes),'.'],check=True)
    env=os.environ.copy();env['JAVA_HOME']=str(args.jdk25)
    command=[str(ROOT/'gradlew.bat')]
    if args.loader!='fabric':command+=['-p',str(ROOT/'platforms'/args.loader)]
    tasks=['runServer','runClient'] if args.loader=='forge' else ['runRuntimeServer','runRuntimeClient']
    command+=tasks+['-I',str(ROOT/'tools/runtime-export.init.gradle'),'-PruntimeExport','-PruntimeRole=Luis',f'-PruntimePort={port}','--no-configuration-cache','--max-workers=2']
    subprocess.run(command,cwd=ROOT,env=env,check=True)
    folder=home/'commands'
    base=json.loads((folder/'Luis.json').read_text())
    original=[json.loads(line) for line in (folder/'Luis-args.txt').read_text().splitlines()]
    if args.loader=='forge':
        assets=Path.home()/'.gradle/caches/neoformruntime/assets'
        # ForgeGradle 7 metadata leaves its legacy asset placeholders unresolved.
        # Use the asset index shared with NeoFormRuntime (Minecraft 26.2).
        original=[s.replace('{asset_index}','32').replace('{assets_root}',str(assets)) for s in original]
    for role in ['Luis','Pepe','Audit']:
        config=copy.deepcopy(base);config['cwd']=str(Path(base['cwd']).with_name(role))
        cloned=[role if s=='Luis' else s for s in original]
        if args.loader=='fabric':
            cloned=[s.replace('\\Luis','\\'+role).replace('/Luis','/'+role) if s.startswith('-D') else s for s in cloned]
        for i,s in enumerate(cloned):
            if s.startswith('@'):
                source=Path(s[1:]);target=folder/f'{role}-program-args.txt'
                target.write_text(source.read_text().replace('\nLuis\n',f'\n{role}\n'))
                cloned[i]='@'+str(target)
        argfile=folder/f'{role}-args.txt';argfile.write_text('\n'.join(json.dumps(s) for s in cloned))
        config['command'][1]='@'+str(argfile)
        (folder/f'{role}.json').write_text(json.dumps(config,indent=2))
    print('Prepared',args.loader,'server and three clients. Run tools/runtime-tests.py',args.loader)

if __name__=='__main__':main()
