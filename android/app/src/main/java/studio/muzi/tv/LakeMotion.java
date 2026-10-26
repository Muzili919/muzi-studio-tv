package studio.muzi.tv;

/** Continuous surface displacement; coastline weights are calculated once per layout. */
final class LakeMotion {
 final int cols,rows; final float[] base,weights,vertices;final float width,height;
 LakeMotion(float[] shore,int cols,int rows,float top,float bottom,int width,int height){this.cols=cols;this.rows=rows;this.width=width;this.height=height;int count=(cols+1)*(rows+1);base=new float[count*2];vertices=new float[count*2];weights=new float[count];int n=0;for(int y=0;y<=rows;y++)for(int x=0;x<=cols;x++){float u=x/(float)cols,v=top+(bottom-top)*y/rows;base[n*2]=u*width;base[n*2+1]=v*height;float depth=clamp((v-.595f)/.26f);float edge=edge(shore,u,v);weights[n]=edge<0?0:smooth(edge/.018f)*smooth(depth/.16f)*(.25f+depth*.75f);n++;}}
 static float clamp(float x){return Math.max(0,Math.min(1,x));}static float smooth(float x){x=clamp(x);return x*x*(3-2*x);}
 static float edge(float[] polygon,float x,float y){boolean in=false;float distance=2;for(int i=0,j=polygon.length-2;i<polygon.length;j=i,i+=2){float ax=polygon[j],ay=polygon[j+1],bx=polygon[i],by=polygon[i+1],dx=bx-ax,dy=by-ay;float z=dx*dx+dy*dy;float q=z==0?0:clamp(((x-ax)*dx+(y-ay)*dy)/z);float ex=x-ax-q*dx,ey=y-ay-q*dy;distance=Math.min(distance,(float)Math.sqrt(ex*ex+ey*ey));if((ay>y)!=(by>y)&&x<(bx-ax)*(y-ay)/(by-ay)+ax)in=!in;}return in?distance:-distance;}
 float[] frame(float t){for(int i=0;i<weights.length;i++){float u=base[i*2]/width,v=base[i*2+1]/height,k=weights[i];float phase=v*95f+(float)Math.sin(u*12f)*.65f;float swell=(float)Math.sin(phase-t*.65f)+.32f*(float)Math.sin(v*173f+u*18f+t*.43f);vertices[i*2]=base[i*2]-(float)Math.sin(v*81f+u*9f-t*.47f)*.00032f*width*k;vertices[i*2+1]=base[i*2+1]-swell*.00085f*height*k;}return vertices;}
}
