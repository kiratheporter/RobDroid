package com.robrun.launcher;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;

public class GameActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        WebView webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.setWebViewClient(new WebViewClient());

        String user = getIntent().getStringExtra("steam_user");
        String appId = getIntent().getStringExtra("app_id");
        boolean online = getIntent().getBooleanExtra("online_mode", true);
        boolean optimize = getIntent().getBooleanExtra("optimize_mode", true);
        String gamePath = getIntent().getStringExtra("game_path");

        if (user == null || user.isEmpty()) user = "SteamRunner";
        if (appId == null || appId.isEmpty()) appId = "0";

        double scale = optimize ? 0.75 : 1.0;

        String html = "<!DOCTYPE html><html><head>" +
            "<meta name='viewport' content='width=device-width, initial-scale=1.0, user-scalable=no'>" +
            "<style>" +
            "*{box-sizing:border-box;}" +
            "body{margin:0;background:#0e121b;color:#fff;font-family:sans-serif;overflow:hidden;user-select:none;-webkit-user-select:none;}" +
            ".bw{filter:grayscale(100%) contrast(135%);}" +
            "#topBar{position:fixed;top:8px;left:8px;right:8px;z-index:30;display:flex;gap:6px;align-items:center;flex-wrap:wrap;}" +
            "#hud{background:rgba(0,0,0,0.65);padding:6px 10px;border-radius:6px;font-size:11px;}" +
            ".topIconBtn{width:36px;height:36px;border-radius:8px;border:1px solid rgba(255,255,255,0.45);background:rgba(15,15,15,0.9);color:#fff;font-size:16px;display:flex;align-items:center;justify-content:center;cursor:pointer;}" +
            ".topIconBtn.activeState,.topIconBtn.hiddenMode{background:#fff;color:#000;border-color:#000;}" +
            "#lastAction{margin-left:auto;background:rgba(255,255,255,0.12);border:1px solid rgba(255,255,255,0.3);color:#fff;padding:6px 10px;border-radius:6px;font-size:11px;}" +
            ".hidden{display:none !important;}" +
            "#quickPanel{position:fixed;top:50px;left:8px;width:min(330px,92vw);max-height:56vh;overflow-y:auto;z-index:25;background:rgba(10,10,10,0.94);border:1px solid rgba(255,255,255,0.3);border-radius:10px;padding:8px;}" +
            ".catRowTitle{font-size:10px;color:#ccc;margin:6px 0 4px;letter-spacing:0.5px;}" +
            ".catRowTitle:first-child{margin-top:0;}" +
            ".iconGrid{display:grid;grid-template-columns:repeat(6,1fr);gap:5px;}" +
            ".iBtn{height:38px;border-radius:7px;border:1px solid rgba(255,255,255,0.35);background:#141414;color:#fff;font-size:15px;display:flex;align-items:center;justify-content:center;touch-action:none;}" +
            ".iBtn:active,.iBtn.active{background:#fff;color:#000;}" +
            "#touchPad{position:fixed;bottom:12px;left:12px;right:12px;z-index:20;display:flex;justify-content:space-between;align-items:flex-end;pointer-events:none;}" +
            ".dpad{display:grid;grid-template-columns:repeat(3,46px);grid-template-rows:repeat(3,42px);gap:4px;pointer-events:auto;}" +
            ".dpad .empty{visibility:hidden;}" +
            ".dBtn{background:rgba(15,15,15,0.88);color:#fff;border:1px solid rgba(255,255,255,0.4);border-radius:8px;font-size:15px;display:flex;align-items:center;justify-content:center;touch-action:none;}" +
            ".dBtn:active,.dBtn.active{background:#fff;color:#000;}" +
            ".rightControls{display:flex;flex-direction:column;align-items:flex-end;gap:6px;pointer-events:auto;}" +
            ".slotRow{display:flex;gap:4px;}" +
            ".sBtn{width:32px;height:28px;border-radius:6px;border:1px solid rgba(255,255,255,0.35);background:rgba(15,15,15,0.88);color:#fff;font-size:11px;font-weight:bold;touch-action:none;}" +
            ".sBtn.selected,.sBtn:active,.sBtn.active{background:#fff;color:#000;}" +
            ".baseActRow{display:flex;gap:5px;}" +
            "#weaponPad{display:grid;grid-template-columns:repeat(4,46px);gap:5px;}" +
            ".aBtn{width:46px;height:42px;border-radius:9px;border:1px solid rgba(255,255,255,0.4);background:rgba(15,15,15,0.9);color:#fff;font-size:15px;display:flex;align-items:center;justify-content:center;touch-action:none;}" +
            ".aBtn:active,.aBtn.active{background:#fff;color:#000;}" +
            "canvas{display:block;width:100vw;height:100vh;}" +
            "</style></head><body>" +
            "<div id='topBar' class='bw'>" +
            "<button id='togglePadBtn' class='topIconBtn' type='button' title='Tuslari Gizle/Goster'>👁</button>" +
            "<button id='toggleMenuBtn' class='topIconBtn' type='button' title='Kisayollar'>⌨</button>" +
            "<button id='toggleWeaponTestBtn' class='topIconBtn' type='button' title='Silah Algilama'>🔫</button>" +
            "<div id='hud'>ROBDROID | AppID: " + appId + " | User: " + user +
            " | Online: " + (online ? "ON" : "OFF") +
            " | Opt: " + (optimize ? "75% / 60FPS" : "100% / 60FPS") + "</div>" +
            "<div id='lastAction'>◌</div>" +
            "</div>" +

            "<div id='quickPanel' class='hidden bw'>" +
            "<div class='catRowTitle'>◼◼◼</div>" +
            "<div class='iconGrid'>" +
            "<button class='iBtn' data-key='Shift' data-label='⚡ Shift'>🏃</button>" +
            "<button class='iBtn' data-key='E' data-label='✋ E'>✋</button>" +
            "<button class='iBtn' data-key='F_HOLD' data-label='🎭 F'>🎭</button>" +
            "<button class='iBtn' data-key='Q' data-label='🏳 Q'>🏳</button>" +
            "<button class='iBtn' data-key='G' data-label='💸 G'>💸</button>" +
            "<button class='iBtn' data-key='Alt' data-label='🚧 Alt'>🚧</button>" +
            "</div>" +

            "<div class='catRowTitle'>🔫⚔</div>" +
            "<div class='iconGrid'>" +
            "<button class='iBtn wOnly' data-key='RMB' data-label='🎯 RMB'>🎯</button>" +
            "<button class='iBtn wOnly' data-key='B' data-label='👁 B'>👁</button>" +
            "<button class='iBtn wOnly' data-key='X' data-label='↔ X'>↔</button>" +
            "<button class='iBtn wOnly' data-key='R' data-label='🔄 R'>🔄</button>" +
            "<button class='iBtn wOnly' data-key='V' data-label='👊 V'>👊</button>" +
            "<button class='iBtn wOnly' data-key='Z' data-label='💼 Z'>💼</button>" +
            "<button class='iBtn' data-key='[' data-label='⏮ ['>⏮</button>" +
            "<button class='iBtn' data-key=']' data-label='⏭ ]'>⏭</button>" +
            "<button class='iBtn' data-key='G_EMOTE' data-label='🪩 G'>🪩</button>" +
            "</div>" +

            "<div class='catRowTitle'>🎒📦</div>" +
            "<div class='iconGrid'>" +
            "<button class='iBtn' data-key='Tab' data-label='🎒 Tab'>🎒</button>" +
            "<button class='iBtn' data-key='Ctrl+LMB' data-label='⇄ Ctrl+LMB'>⇄</button>" +
            "<button class='iBtn' data-key='G_HOLD' data-label='📥 G'>📥</button>" +
            "<button class='iBtn' data-key='X+LMB' data-label='🗑 X+LMB'>🗑</button>" +
            "<button class='iBtn' data-key='Alt_HOLD' data-label='✂ Alt'>✂</button>" +
            "</div>" +

            "<div class='catRowTitle'>🗺💬</div>" +
            "<div class='iconGrid'>" +
            "<button class='iBtn' data-key='M' data-label='🗺 M'>🗺</button>" +
            "<button class='iBtn' data-key='MMB' data-label='📍 MMB'>📍</button>" +
            "<button class='iBtn' data-key='T' data-label='🎙 T'>🎙</button>" +
            "<button class='iBtn' data-key='Enter' data-label='💬 Enter'>💬</button>" +
            "<button class='iBtn' data-key='K' data-label='📖 K'>📖</button>" +
            "<button class='iBtn' data-key='J' data-label='✨ J'>✨</button>" +
            "<button class='iBtn' data-key='H' data-label='📦 H'>📦</button>" +
            "<button class='iBtn' data-key='Q_MAIL' data-label='✉ Q'>✉</button>" +
            "<button class='iBtn' data-key='P' data-label='👥 P'>👥</button>" +
            "<button class='iBtn' data-key='L' data-label='🛡 L'>🛡</button>" +
            "<button class='iBtn' data-key='U' data-label='📋 U'>📋</button>" +
            "</div>" +
            "</div>" +

            "<div id='touchPad' class='bw'>" +
            "<div class='dpad'>" +
            "<div class='empty'></div>" +
            "<button id='btnW' class='dBtn' type='button'>▲</button>" +
            "<div class='empty'></div>" +
            "<button id='btnA' class='dBtn' type='button'>◀</button>" +
            "<button id='btnS' class='dBtn' type='button'>▼</button>" +
            "<button id='btnD' class='dBtn' type='button'>▶</button>" +
            "<button id='btnCtrl' class='dBtn' type='button'>⬇</button>" +
            "<button id='btnSpace' class='dBtn' type='button'>⬆</button>" +
            "<button id='btnShift' class='dBtn' type='button'>⚡</button>" +
            "</div>" +

            "<div class='rightControls'>" +
            "<div class='slotRow'>" +
            "<button class='sBtn' data-key='1' data-label='1'>1</button>" +
            "<button class='sBtn' data-key='2' data-label='2'>2</button>" +
            "<button class='sBtn' data-key='3' data-label='3'>3</button>" +
            "<button class='sBtn' data-key='4' data-label='4'>4</button>" +
            "<button class='sBtn' data-key='5' data-label='5'>5</button>" +
            "<button class='sBtn' data-key='6' data-label='6'>6</button>" +
            "</div>" +
            "<div id='weaponPad' class='hidden'>" +
            "<button id='btnLMB' class='aBtn' type='button'>💥</button>" +
            "<button id='btnRMB' class='aBtn' type='button'>🎯</button>" +
            "<button id='btnR' class='aBtn' type='button'>🔄</button>" +
            "<button id='btnZ' class='aBtn' type='button'>💼</button>" +
            "</div>" +
            "<div class='baseActRow'>" +
            "<button id='btnE' class='aBtn' type='button'>✋</button>" +
            "<button id='btnF' class='aBtn' type='button'>📦</button>" +
            "<button id='btnTab' class='aBtn' type='button'>🎒</button>" +
            "</div>" +
            "</div>" +
            "</div>" +

            "<canvas id='c'></canvas>" +
            "<script>" +
            "const canvas=document.getElementById('c');" +
            "const ctx=canvas.getContext('2d');" +
            "const scale=" + scale + ";" +
            "const lastActionEl=document.getElementById('lastAction');" +
            "const togglePadBtn=document.getElementById('togglePadBtn');" +
            "const toggleMenuBtn=document.getElementById('toggleMenuBtn');" +
            "const toggleWeaponTestBtn=document.getElementById('toggleWeaponTestBtn');" +
            "const touchPad=document.getElementById('touchPad');" +
            "const quickPanel=document.getElementById('quickPanel');" +
            "const weaponPad=document.getElementById('weaponPad');" +

            "let controlsVisible=true;" +
            "let hasWeapon=false;" +
            "let keyW=false,keyA=false,keyS=false,keyD=false,keyShift=false,keyCtrl=false,firing=false;" +
            "let activeSlot='0';" +

            "function syncWeaponUI(){" +
            "weaponPad.classList.toggle('hidden',!hasWeapon);" +
            "toggleWeaponTestBtn.classList.toggle('activeState',hasWeapon);" +
            "document.querySelectorAll('.iBtn.wOnly').forEach(btn=>btn.classList.toggle('hidden',!hasWeapon));" +
            "if(!hasWeapon){firing=false;}" +
            "}" +

            "window.robdroidSetWeaponState=function(equipped,slotCode){" +
            "hasWeapon=!!equipped;" +
            "if(slotCode!==undefined){activeSlot=String(slotCode);}" +
            "if(!hasWeapon && (activeSlot==='1' || activeSlot==='2')){activeSlot='0';}" +
            "document.querySelectorAll('.sBtn').forEach(s=>s.classList.toggle('selected',s.dataset.key===activeSlot));" +
            "syncWeaponUI();" +
            "lastActionEl.textContent=(hasWeapon?'🔫':'◌')+' S:'+activeSlot;" +
            "};" +

            "window.addEventListener('robdroidGameState',(ev)=>{" +
            "const d=ev.detail||{};" +
            "if(typeof d.hasWeapon==='boolean'){window.robdroidSetWeaponState(d.hasWeapon,d.slot);}" +
            "});" +

            "function sendInput(code,label,isDown){" +
            "if(!controlsVisible)return;" +
            "if(label)lastActionEl.textContent=(isDown?'▪ ':'▫ ')+label;" +
            "window.dispatchEvent(new CustomEvent('robdroidInput',{detail:{code,label,isDown,hasWeapon,slot:activeSlot}}));" +
            "}" +

            "togglePadBtn.addEventListener('click',()=>{" +
            "controlsVisible=!controlsVisible;" +
            "if(!controlsVisible){" +
            "keyW=keyA=keyS=keyD=keyShift=keyCtrl=firing=false;" +
            "quickPanel.classList.add('hidden');" +
            "toggleMenuBtn.classList.add('hidden');" +
            "toggleWeaponTestBtn.classList.add('hidden');" +
            "lastActionEl.textContent='◌';" +
            "}else{" +
            "toggleMenuBtn.classList.remove('hidden');" +
            "toggleWeaponTestBtn.classList.remove('hidden');" +
            "lastActionEl.textContent='●';" +
            "}" +
            "touchPad.classList.toggle('hidden',!controlsVisible);" +
            "togglePadBtn.classList.toggle('hiddenMode',!controlsVisible);" +
            "togglePadBtn.textContent=controlsVisible?'👁':'🚫';" +
            "});" +

            "toggleMenuBtn.addEventListener('click',()=>{" +
            "if(!controlsVisible)return;" +
            "quickPanel.classList.toggle('hidden');" +
            "});" +

            "toggleWeaponTestBtn.addEventListener('click',()=>{" +
            "if(!controlsVisible)return;" +
            "const nextState=!hasWeapon;" +
            "window.robdroidSetWeaponState(nextState,nextState?(activeSlot==='1'||activeSlot==='2'?activeSlot:'1'):'0');" +
            "sendInput(nextState?'WEAPON_EQUIP':'WEAPON_HOLSTER',nextState?'🔫':'💼',true);" +
            "});" +

            "function bindHold(el,onDown,onUp){" +
            "if(!el)return;" +
            "const down=(e)=>{e.preventDefault();if(!controlsVisible)return;el.classList.add('active');onDown();};" +
            "const up=(e)=>{e.preventDefault();el.classList.remove('active');onUp();};" +
            "el.addEventListener('touchstart',down,{passive:false});" +
            "el.addEventListener('touchend',up,{passive:false});" +
            "el.addEventListener('touchcancel',up,{passive:false});" +
            "el.addEventListener('mousedown',down);" +
            "el.addEventListener('mouseup',up);" +
            "el.addEventListener('mouseleave',up);" +
            "}" +

            "function resize(){canvas.width=Math.max(1,window.innerWidth*scale);canvas.height=Math.max(1,window.innerHeight*scale);}" +
            "resize();window.addEventListener('resize',resize);" +

            "let playerX=0.18,playerY=0.72,vy=0,grounded=true,score=0,obstacles=[];" +
            "function jump(){if(grounded){vy=-0.018;grounded=false;keyCtrl=false;}}" +

            "bindHold(document.getElementById('btnW'),()=>{keyW=true;sendInput('W','▲ W',true);},()=>{keyW=false;sendInput('W','▲ W',false);});" +
            "bindHold(document.getElementById('btnA'),()=>{keyA=true;sendInput('A','◀ A',true);},()=>{keyA=false;sendInput('A','◀ A',false);});" +
            "bindHold(document.getElementById('btnS'),()=>{keyS=true;sendInput('S','▼ S',true);},()=>{keyS=false;sendInput('S','▼ S',false);});" +
            "bindHold(document.getElementById('btnD'),()=>{keyD=true;sendInput('D','▶ D',true);},()=>{keyD=false;sendInput('D','▶ D',false);});" +
            "bindHold(document.getElementById('btnCtrl'),()=>{keyCtrl=true;sendInput('Ctrl','⬇ Ctrl',true);},()=>{keyCtrl=false;sendInput('Ctrl','⬇ Ctrl',false);});" +
            "bindHold(document.getElementById('btnShift'),()=>{keyShift=true;sendInput('Shift','⚡ Shift',true);},()=>{keyShift=false;sendInput('Shift','⚡ Shift',false);});" +
            "bindHold(document.getElementById('btnLMB'),()=>{if(!hasWeapon)return;firing=true;sendInput('LMB','💥 LMB',true);},()=>{firing=false;sendInput('LMB','💥 LMB',false);});" +
            "bindHold(document.getElementById('btnRMB'),()=>{if(!hasWeapon)return;sendInput('RMB','🎯 RMB',true);},()=>{if(!hasWeapon)return;sendInput('RMB','🎯 RMB',false);});" +

            "const btnSpace=document.getElementById('btnSpace');" +
            "btnSpace.addEventListener('touchstart',(e)=>{e.preventDefault();if(!controlsVisible)return;jump();sendInput('Space','⬆ Space',true);},{passive:false});" +
            "btnSpace.addEventListener('mousedown',(e)=>{e.preventDefault();if(!controlsVisible)return;jump();sendInput('Space','⬆ Space',true);});" +

            "const btnE=document.getElementById('btnE');" +
            "btnE.addEventListener('touchstart',(e)=>{e.preventDefault();if(!controlsVisible)return;sendInput('E','✋ E',true);},{passive:false});" +
            "btnE.addEventListener('mousedown',(e)=>{e.preventDefault();if(!controlsVisible)return;sendInput('E','✋ E',true);});" +

            "let fTimer=null,fLong=false;" +
            "const btnF=document.getElementById('btnF');" +
            "btnF.addEventListener('touchstart',(e)=>{e.preventDefault();if(!controlsVisible)return;fLong=false;btnF.classList.add('active');" +
            "fTimer=setTimeout(()=>{fLong=true;sendInput('F_HOLD','🎭 F',true);},450);},{passive:false});" +
            "btnF.addEventListener('touchend',(e)=>{e.preventDefault();clearTimeout(fTimer);btnF.classList.remove('active');if(!controlsVisible)return;" +
            "if(!fLong)sendInput('F','📦 F',true);},{passive:false});" +

            "const btnR=document.getElementById('btnR');" +
            "btnR.addEventListener('touchstart',(e)=>{e.preventDefault();if(!controlsVisible||!hasWeapon)return;sendInput('R','🔄 R',true);},{passive:false});" +

            "const btnZ=document.getElementById('btnZ');" +
            "btnZ.addEventListener('touchstart',(e)=>{e.preventDefault();if(!controlsVisible||!hasWeapon)return;" +
            "window.robdroidSetWeaponState(false,'0');sendInput('Z','💼 Z',true);},{passive:false});" +

            "const btnTab=document.getElementById('btnTab');" +
            "btnTab.addEventListener('touchstart',(e)=>{e.preventDefault();if(!controlsVisible)return;sendInput('Tab','🎒 Tab',true);},{passive:false});" +

            "document.querySelectorAll('.sBtn').forEach(b=>{" +
            "b.addEventListener('touchstart',(e)=>{e.preventDefault();if(!controlsVisible)return;" +
            "const code=b.dataset.key;" +
            "const weaponSlot=(code==='1'||code==='2');" +
            "window.robdroidSetWeaponState(weaponSlot,code);" +
            "sendInput(code,b.dataset.label,true);},{passive:false});" +
            "});" +

            "document.querySelectorAll('.iBtn').forEach(b=>{" +
            "bindHold(b,()=>{" +
            "const code=b.dataset.key;" +
            "if(code==='Z'){window.robdroidSetWeaponState(false,'0');}" +
            "sendInput(code,b.dataset.label,true);" +
            "},()=>sendInput(b.dataset.key,b.dataset.label,false));" +
            "});" +

            "syncWeaponUI();" +
            "let last=performance.now();const stepMs=1000/60;" +
            "function loop(now){if(now-last>=stepMs){last=now;update();draw();}requestAnimationFrame(loop);}" +
            "function update(){score++;" +
            "if(controlsVisible){" +
            "const spd=keyShift?0.013:0.0075;" +
            "if(keyA)playerX=Math.max(0.05,playerX-spd);" +
            "if(keyD)playerX=Math.min(0.85,playerX+spd);" +
            "if(keyW)playerY=Math.max(0.45,playerY-0.005);" +
            "if(keyS&&grounded)playerY=Math.min(0.75,playerY+0.005);" +
            "}" +
            "if(!grounded){vy+=0.00085;playerY+=vy;if(playerY>=0.72){playerY=0.72;vy=0;grounded=true;}}" +
            "if(score%95===0)obstacles.push({x:1.05,w:0.04,h:0.08});" +
            "for(let i=obstacles.length-1;i>=0;i--){obstacles[i].x-=0.012;if(obstacles[i].x<-0.1)obstacles.splice(i,1);}}" +
            "function draw(){ctx.fillStyle='#0e121b';ctx.fillRect(0,0,canvas.width,canvas.height);" +
            "const gy=canvas.height*0.76;ctx.fillStyle='#253047';ctx.fillRect(0,gy,canvas.width,canvas.height-gy);" +
            "const ps=canvas.height*0.06;const ph=keyCtrl&&grounded?ps*0.55:ps;" +
            "const px=canvas.width*playerX,py=canvas.height*playerY-ph;" +
            "ctx.fillStyle=firing?'#ffd166':(hasWeapon?'#00e5ff':'#8d99ae');ctx.fillRect(px,py,ps,ph);" +
            "ctx.fillStyle='#ff4757';obstacles.forEach(o=>{ctx.fillRect(canvas.width*o.x,gy-canvas.height*o.h,canvas.width*o.w,canvas.height*o.h);});" +
            "ctx.fillStyle='#fff';ctx.font=(canvas.height*0.035)+'px sans-serif';" +
            "ctx.fillText('WPN:'+(hasWeapon?'ON':'OFF')+' | S:'+activeSlot+' | '+score,canvas.width*0.58,canvas.height*0.18);}" +
            "requestAnimationFrame(loop);" +
            "</script></body></html>";

        webView.loadDataWithBaseURL("file://" + gamePath + "/", html, "text/html", "UTF-8", null);
        setContentView(webView);
    }
}
