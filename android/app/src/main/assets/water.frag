precision highp float;
 varying vec2 uv;uniform sampler2D wall;uniform sampler2D lakeMask;uniform float time; uniform float animate;
 void main(){
 float depth=clamp((uv.y-0.595)/0.265,0.0,1.0);
 float weight=texture2D(lakeMask,uv).a*smoothstep(0.0,0.11,depth);
 if(animate<0.5||weight<0.001){gl_FragColor=texture2D(wall,uv);return;}
 float z=log(1.0+depth*5.0);float cycle=time*0.3926990817;
 float wind=sin(uv.x*15.0+z*4.0+cycle)*0.7+sin(uv.x*31.0-z*7.0-cycle*2.0)*0.25;
 float p1=z*24.0+uv.x*6.0-cycle*3.0+wind*0.35;
 float p2=z*83.0+uv.x*25.0-cycle*7.0+sin(uv.x*17.0+z*11.0+cycle)*0.8;
 float p3=z*61.0-uv.x*32.0+cycle*2.0;
 float swell=sin(p1),ripple=sin(p2),crossWave=sin(p3);
 float amplitude=0.0017+0.0033*pow(depth,0.9);
 vec2 shift=vec2((sin(p1*0.5+cycle*1.5)*0.5+crossWave*0.3)*0.0014*(0.3+depth),(swell*0.70+ripple*0.22+crossWave*0.08)*amplitude)*weight;
 vec3 col=texture2D(wall,uv+shift).rgb;
 float slope=cos(p1)*0.52+cos(p2)*0.27+cos(p3)*0.13;
 float glint=smoothstep(0.90,1.03,ripple+0.10*sin(uv.x*113.0+z*19.0-cycle*3.0));
 float broken=0.35+0.65*smoothstep(-0.4,0.9,sin(uv.x*37.0-z*9.0+cycle*2.0));
 col*=1.0+weight*slope*0.10;
 col+=vec3(0.15,0.21,0.26)*glint*broken*weight*(0.020+depth*0.065);
 gl_FragColor=vec4(col,1.0);
 }