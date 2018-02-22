attribute vec4 aPosition;
uniform mat4 uMatrix;
uniform mat4 uTextureMatrix;
attribute vec4 aTextureCoordinates;
varying vec2 vTextureCoordinates;

void main()
{
    vTextureCoordinates = (uTextureMatrix * aTextureCoordinates).xy;
    //vTextureCoordinates = aTextureCoordinates;
    gl_Position = uMatrix * aPosition;
}