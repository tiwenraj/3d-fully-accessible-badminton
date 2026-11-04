package com.tiwenraj.football;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import java.nio.*;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** 3D badminton court and basic shuttle rally. */
public final class FootballRenderer implements GLSurfaceView.Renderer {
 public interface ScoreListener{void onScore(int score);}
 private final Object lock=new Object(); private final ScoreListener listener;
 private float px=0,pz=-6,sx=0,sy=2,sz=-4,vx,vy,vz,mx,mz,ox=0,od=1;
 private boolean flying=false,contrast=false; private int score=0,program,pos,mvp,col;
 private FloatBuffer vertices; private final float[] proj=new float[16],view=new float[16],vp=new float[16],model=new float[16],out=new float[16]; private long last;
 public FootballRenderer(ScoreListener l){listener=l;}
 public void setMovement(float x,float z){synchronized(lock){mx=x;mz=z;}}
 public void nudge(float x,float z){synchronized(lock){px=clamp(px+x,-3.5f,3.5f);pz=clamp(pz+z,-8.5f,-1);}}
 public void kick(){synchronized(lock){if(!flying&&Math.hypot(sx-px,sz-pz)<3.5){flying=true;vx=(ox-px)*.2f;vy=5;vz=9;}}}
 public void setHighContrast(boolean b){synchronized(lock){contrast=b;}}
 public void onSurfaceCreated(GL10 g,EGLConfig c){
  GLES20.glClearColor(.03f,.07f,.1f,1);GLES20.glEnable(GLES20.GL_DEPTH_TEST);
  String vs="uniform mat4 u;attribute vec3 p;void main(){gl_Position=u*vec4(p,1.0);}";
  String fs="precision mediump float;uniform vec4 c;void main(){gl_FragColor=c;}";
  int a=shader(35633,vs),b=shader(35632,fs);program=GLES20.glCreateProgram();GLES20.glAttachShader(program,a);GLES20.glAttachShader(program,b);GLES20.glLinkProgram(program);
  pos=GLES20.glGetAttribLocation(program,"p");mvp=GLES20.glGetUniformLocation(program,"u");col=GLES20.glGetUniformLocation(program,"c");vertices=buffer(data());
 }
 public void onSurfaceChanged(GL10 g,int w,int h){GLES20.glViewport(0,0,w,Math.max(1,h));float a=(float)w/Math.max(1,h);Matrix.frustumM(proj,0,-a,a,-1,1,2,80);}
 public void onDrawFrame(GL10 g){
  long now=System.nanoTime();float dt=last==0?.016f:Math.min((now-last)/1e9f,.05f);last=now;int scored=step(dt);
  GLES20.glClear(16640);Matrix.setLookAtM(view,0,0,14,-23,0,0,0,0,1,0);Matrix.multiplyMM(vp,0,proj,0,view,0);
  court();net();person(px,pz,new float[]{.08f,.4f,.95f,1});person(ox,6.8f,new float[]{.9f,.25f,.1f,1});
  box(sx,sy,sz,.18f,.18f,.18f,new float[]{.95f,.75f,.15f,1});box(sx,sy+.12f,sz-.12f,.28f,.22f,.24f,new float[]{.95f,.97f,1,1});
  if(scored>=0&&listener!=null)listener.onScore(scored);
 }
 private int step(float d){synchronized(lock){
  px=clamp(px+mx*5*d,-3.5f,3.5f);pz=clamp(pz+mz*5*d,-8.5f,-1);
  ox+=od*2*d;if(Math.abs(ox)>2.7)od=-od;
  if(flying){sx+=vx*d;sy+=vy*d;sz+=vz*d;vy-=9.8f*d;if(sz>7.5f){score++;reset();return score;}if(sy<.2f||Math.abs(sx)>4||sz< -9)reset();}
  else{sx=px+.3f;sy=2;sz=pz+1;}
  return -1;
 }}
 private void reset(){flying=false;vx=vy=vz=0;sx=px+.3f;sy=2;sz=pz+1;}
 private void court(){boolean h; synchronized(lock){h=contrast;}float[] floor=h?new float[]{.01f,.04f,.07f,1}:new float[]{.07f,.32f,.22f,1};float[] l=h?new float[]{1,.9f,.05f,1}:new float[]{1,1,1,1};
  box(0,-.12f,0,9,.2f,19,floor);box(-4.4f,.02f,0,.05f,.03f,19,l);box(4.4f,.02f,0,.05f,.03f,19,l);box(0,.02f,-9.5f,8.8f,.03f,.05f,l);box(0,.02f,9.5f,8.8f,.03f,.05f,l);
  box(0,.02f,-2.2f,8.8f,.03f,.05f,l);box(0,.02f,2.2f,8.8f,.03f,.05f,l);box(-2.2f,.02f,0,.05f,.03f,4.4f,l);box(2.2f,.02f,0,.05f,.03f,4.4f,l);
 }
 private void net(){float[] n={.9f,.95f,1,1};box(0,1.15f,0,8.8f,.08f,.08f,n);box(-4.35f,.65f,0,.08f,1.3f,.08f,n);box(4.35f,.65f,0,.08f,1.3f,.08f,n);}
 private void person(float x,float z,float[] shirt){box(x,.9f,z,.58f,.85f,.38f,shirt);box(x-.15f,.28f,z,.18f,.45f,.2f,new float[]{.08f,.1f,.15f,1});box(x+.15f,.28f,z,.18f,.45f,.2f,new float[]{.08f,.1f,.15f,1});box(x,1.55f,z,.34f,.34f,.34f,new float[]{.7f,.48f,.32f,1});box(x+.5f,1.2f,z+.2f,.08f,.08f,.8f,new float[]{.95f,.95f,1,1});}
 private void box(float x,float y,float z,float a,float b,float c,float[] color){Matrix.setIdentityM(model,0);Matrix.translateM(model,0,x,y,z);Matrix.scaleM(model,0,a,b,c);Matrix.multiplyMM(out,0,vp,0,model,0);GLES20.glUseProgram(program);vertices.position(0);GLES20.glEnableVertexAttribArray(pos);GLES20.glVertexAttribPointer(pos,3,5126,false,12,vertices);GLES20.glUniformMatrix4fv(mvp,1,false,out,0);GLES20.glUniform4fv(col,1,color,0);GLES20.glDrawArrays(4,0,36);GLES20.glDisableVertexAttribArray(pos);}
 private int shader(int t,String s){int x=GLES20.glCreateShader(t);GLES20.glShaderSource(x,s);GLES20.glCompileShader(x);return x;}
 private FloatBuffer buffer(float[] a){FloatBuffer b=ByteBuffer.allocateDirect(a.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();b.put(a).position(0);return b;}
 private float[] data(){float[][] f={{-.5f,-.5f,.5f,.5f,-.5f,.5f,.5f,.5f,.5f,-.5f,.5f,.5f},{.5f,-.5f,-.5f,-.5f,-.5f,-.5f,-.5f,.5f,-.5f,.5f,.5f,-.5f},{-.5f,.5f,.5f,.5f,.5f,.5f,.5f,.5f,-.5f,-.5f,.5f,-.5f},{-.5f,-.5f,-.5f,.5f,-.5f,-.5f,.5f,-.5f,.5f,-.5f,-.5f,.5f},{.5f,-.5f,.5f,.5f,-.5f,-.5f,.5f,.5f,-.5f,.5f,.5f,.5f},{-.5f,-.5f,-.5f,-.5f,-.5f,.5f,-.5f,.5f,.5f,-.5f,.5f,-.5f}};float[] o=new float[108];int k=0;int[] q={0,1,2,0,2,3};for(float[] face:f)for(int i:q)for(int j=0;j<3;j++)o[k++]=face[i*3+j];return o;}
 private float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}
}