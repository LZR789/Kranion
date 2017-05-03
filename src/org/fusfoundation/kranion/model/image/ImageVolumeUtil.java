/*
 * The MIT License
 *
 * Copyright 2016 Focused Ultrasound Foundation.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.fusfoundation.kranion.model.image;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import org.fusfoundation.kranion.ImageGradientVolume;
import static org.lwjgl.opengl.GL11.GL_CLAMP;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_RED;
import static org.lwjgl.opengl.GL11.GL_SHORT;
import static org.lwjgl.opengl.GL11.GL_TEXTURE;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_HEIGHT;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WIDTH;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNPACK_ALIGNMENT;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glGetTexLevelParameteri;
import static org.lwjgl.opengl.GL11.glIsTexture;
import static org.lwjgl.opengl.GL11.glPixelStorei;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_3D;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_DEPTH;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_WRAP_R;
import static org.lwjgl.opengl.GL12.glTexImage3D;
import static org.lwjgl.opengl.GL30.GL_R16;
import static org.lwjgl.opengl.GL30.GL_R32F;
import static org.lwjgl.opengl.GL30.GL_R8;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL12.glTexImage3D;

import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author john
 */
public class ImageVolumeUtil {
    public static void buildTexture(ImageVolume image) {
        buildTexture(image, true);
    }
    
    public static void buildTexture(ImageVolume image, boolean doGradientVolume) {
        buildTexture(image, doGradientVolume, 0);
    }
    
    public static void buildTexture(ImageVolume image, boolean doGradientVolume, int frame) {

        
        if (image != null) {            
            //System.out.println("ImageCanvas3D::build texture..");

            Integer tn = (Integer) image.getAttribute("textureName");
//            System.out.println("   textureName = " + tn);

            if (tn != null && tn != -1) {              
                //System.out.println("Got previously built texture = " + tn);
            } else {

                //System.out.println("build new texture");
                ByteBuffer buf = ByteBuffer.allocateDirect(4);
                IntBuffer texName = buf.asIntBuffer();

//                release();

                glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
                glGenTextures(texName);
                int textureName = texName.get(0);

                glBindTexture(GL_TEXTURE_3D, textureName);

                image.setAttribute("textureName", new Integer(textureName));

                glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP);
                glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP);
                glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_CLAMP);
                glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

                int pixelType = image.getVoxelType();
                int width = image.getDimension(0).getSize();
                int height = image.getDimension(1).getSize();
                int depth = image.getDimension(2).getSize();
                int frames = image.getDimension(3).getSize();
                
                int selectedFrame = Math.max(0, Math.min(frames-1, frame));

                //System.out.println("size: " + width + " x " + height);
                int iWidth = width;
                int iHeight = height;
                int texWidth = width;
                int texHeight = height;
                int texDepth = depth;

                // Guess we don't require power of two sizes on modern hardware
                // texWidth = nextPowerOfTwo(texWidth);
                // texHeight = nextPowerOfTwo(texHeight);
                // texDepth = nextPowerOfTwo(texDepth);
                //System.out.println("size: " + texWidth + " x " + texHeight + " x " + texDepth);
                if (pixelType == ImageVolume.UBYTE_VOXEL) {
                    System.out.println("  building 8bit texture");

                    ByteBuffer pixelBuf = image.getByteBuffer();
                    pixelBuf.order(ByteOrder.LITTLE_ENDIAN);
                    //glTexImage3D(GL_TEXTURE_3D, 0, GL_INTENSITY8, texWidth, texHeight, texDepth, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, pixelBuf);
                    //glTexImage3D(GL_TEXTURE_3D, 0, GL_ALPHA8, texWidth, texHeight, texDepth, 0, GL_ALPHA, GL_UNSIGNED_BYTE, pixelBuf);
                     glTexImage3D(GL_TEXTURE_3D, 0, GL_R8, texWidth, texHeight, texDepth, 0, GL_RED, GL_UNSIGNED_BYTE, pixelBuf);
               } else if (pixelType == ImageVolume.USHORT_VOXEL){
                    System.out.println("  building 16bit texture");

                    ByteBuffer tmp = image.getByteBuffer();
                    tmp.order(ByteOrder.LITTLE_ENDIAN);
                    //ShortBuffer pixelBuf = (tmp.asShortBuffer());
                    //glTexImage3D(GL_TEXTURE_3D, 0, GL_INTENSITY16, texWidth, texHeight, texDepth, 0, GL_LUMINANCE, GL_SHORT, pixelBuf);
                    //glTexImage3D(GL_TEXTURE_3D, 0, GL_ALPHA16, texWidth, texHeight, texDepth, 0, GL_ALPHA, GL_SHORT, pixelBuf);
                    glTexImage3D(GL_TEXTURE_3D, 0, GL_R16, texWidth, texHeight, texDepth, 0, GL_RED, GL_SHORT, tmp);
               } else if (pixelType == ImageVolume.FLOAT_VOXEL){
                    System.out.println("  building float32 texture");

                    ByteBuffer tmp = image.getByteBuffer();

                    tmp.position(selectedFrame*width*height*depth*4);
                    tmp.order(ByteOrder.LITTLE_ENDIAN);
                    //ShortBuffer pixelBuf = (tmp.asShortBuffer());
                    //glTexImage3D(GL_TEXTURE_3D, 0, GL_INTENSITY16, texWidth, texHeight, texDepth, 0, GL_LUMINANCE, GL_SHORT, pixelBuf);
                    //glTexImage3D(GL_TEXTURE_3D, 0, GL_ALPHA16, texWidth, texHeight, texDepth, 0, GL_ALPHA, GL_SHORT, pixelBuf);
                    glTexImage3D(GL_TEXTURE_3D, 0, GL_R32F, texWidth, texHeight, texDepth, 0, GL_RED, GL_FLOAT, tmp);
                }
                int value;
                value = glGetTexLevelParameteri(GL_TEXTURE_3D, 0, GL_TEXTURE_WIDTH);
                System.out.println("Texture Width = " + value);
                value = glGetTexLevelParameteri(GL_TEXTURE_3D, 0, GL_TEXTURE_HEIGHT);
                System.out.println("Texture Height = " + value);
                value = glGetTexLevelParameteri(GL_TEXTURE_3D, 0, GL_TEXTURE_DEPTH);
                System.out.println("Texture Depth = " + value);

//                needsTexture = false;
//                glBindTexture(GL_TEXTURE_3D, 0); // TODO: Not sure this is correct

            }
            
        }
        
        // Creates a precalculated image gradient 3D texture
        // and leaves the GL texture name as an image attribute
        // "gradientTexName"
        if (image != null && doGradientVolume) { // && image == this.CTimage) { // calculating gradient volume for both MR and CT to try
            Integer tn = (Integer) image.getAttribute("gradientTexName");
            if (tn == null || tn.intValue() == 0) {
                ImageGradientVolume grad = new ImageGradientVolume();
                grad.setImage(image);
                grad.calculate();
            }
        }

    }
    
    public static void releaseTexture(ImageVolume image) {
        releaseTexture(image, "textureName");
    }
    
    public static void releaseTextures(ImageVolume image) {
        releaseTexture(image, "textureName");
        releaseTexture(image, "gradientTexName");
    }
    
    public static void releaseTexture(ImageVolume image, String texAttribName) {
        if (image != null) {
            Integer tn = (Integer) image.getAttribute(texAttribName);
            if (tn == null) return;
            
            int textureName = tn.intValue();
            
            image.removeAttribute(texAttribName);
            
            if (glIsTexture(textureName)) {
                ByteBuffer texName = ByteBuffer.allocateDirect(4);
                texName.asIntBuffer().put(0, textureName);
                texName.flip();
                glDeleteTextures(texName.asIntBuffer());
            }    
        }
    }
    
    public static void setupImageOrientationInfo(ImageVolume image) {
        float[] imageOrientationV = (float[])image.getAttribute("ImageOrientation");
        if (imageOrientationV == null) {
            return;
        }
        
        Vector3f imageOrientationX = new Vector3f();
        Vector3f imageOrientationY = new Vector3f();
        Vector3f imageOrientationZ = new Vector3f();
        Vector3f imageOriginPosition = new Vector3f();
        Quaternion imageOrientationQ = new Quaternion();
        
        int xsize = image.getDimension(0).getSize();
        int ysize = image.getDimension(1).getSize();
        int zsize = image.getDimension(2).getSize();

        float xres = image.getDimension(0).getSampleWidth(0);
        float yres = image.getDimension(1).getSampleWidth(1);
        float zres = image.getDimension(2).getSampleWidth(2);
        
        imageOrientationX.x = imageOrientationV[0];
        imageOrientationX.y = imageOrientationV[1];
        imageOrientationX.z = imageOrientationV[2];
        
        imageOrientationY.x = imageOrientationV[3];
        imageOrientationY.y = imageOrientationV[4];
        imageOrientationY.z = imageOrientationV[5];
        
        Vector3f.cross(imageOrientationX, imageOrientationY, imageOrientationZ);
        
        image.setAttribute("ImageOrientationX", imageOrientationX);
        image.setAttribute("ImageOrientationY", imageOrientationY);
        
        System.out.println("Image X vector = " + imageOrientationX);
        System.out.println("Image Y vector = " + imageOrientationY);
        
        float[] imagePosition = (float[])image.getAttribute("ImagePosition");
        
        if (imagePosition == null) {
            return;
        }
        
//        imageOriginPosition.x = imagePosition[0] + ((imageOrientationX.x * xsize * xres)/2f + (imageOrientationX.y * ysize * yres)/2f + (imageOrientationX.z * zsize * zres)/2f);
//        imageOriginPosition.y = imagePosition[1] + ((imageOrientationY.x * xsize * xres)/2f + (imageOrientationY.y * ysize * yres)/2f + (imageOrientationY.z * zsize * zres)/2f);
//        imageOriginPosition.z = imagePosition[2] + ((imageOrientationZ.x * xsize * xres)/2f + (imageOrientationZ.y * ysize * yres)/2f + (imageOrientationZ.z * zsize * zres)/2f);
        imageOriginPosition.x = imagePosition[0];
        imageOriginPosition.y = imagePosition[1];
        imageOriginPosition.z = imagePosition[2];
        
        //
        //
        // Trying to align image volume with world coordinate system
        // using a quaternion derived from the axis/angle rotation 
        // between two Z direction vectors
        //////////////////////////////////////////////////////////////////
        
        Vector3f xvec = new Vector3f(imageOrientationX.x, imageOrientationX.y, imageOrientationX.z);
        Vector3f yvec = new Vector3f(imageOrientationY.x, imageOrientationY.y, imageOrientationY.z);
//       
//        Vector3f zvec = Vector3f.cross(xvec, yvec, null).normalise(null);
//        
//        imageOrientationZ.x = zvec.x;
//        imageOrientationZ.y = zvec.y;
//        imageOrientationZ.z = zvec.z;
       
        image.setAttribute("ImageOrientationZ", imageOrientationZ);
        
//        Vector3f stdZvec = new Vector3f(0f, 0f, 1f);
        
//        zvec.set(xvec);
//        stdZvec.set(1, 0, 0);
        
        Quaternion q1 = quatFromVectorAngle(xvec, new Vector3f(1, 0, 0));
        Quaternion q2 = quatFromVectorAngle(yvec, new Vector3f(0, 1, 0));
        
        imageOrientationQ = Quaternion.mul(q2, q1, null);
             
        if (image.getAttribute("ImageOrientationQ") == null) {
            image.setAttribute("ImageOrientationQ", imageOrientationQ);
        }
        
        Vector3f imageTranslation = (Vector3f)image.getAttribute("ImageTranslation");
        if (imageTranslation == null) {
//            image.setAttribute("ImageTranslation", imageOriginPosition);//new Vector3f(0f,0f,0f));
        }
             
        System.out.println("Image orient quat: " + imageOrientationQ);       
        System.out.println("Image position = " + imageOriginPosition);
    }
    
    private static Quaternion quatFromVectorAngle(Vector3f v1, Vector3f v2)
    {
        Quaternion result = new Quaternion();
        
        float angle = (float)Math.acos(Vector3f.dot(v1, v2)/(v1.length() * v2.length()));
        if (angle != 0f) {
            Vector3f axis = (Vector3f.cross(v1, v2, null).normalise(null));
            float s = (float)Math.sin(angle/2);

            System.out.println("angle = " + angle);
            System.out.println("axis = " + axis);
            System.out.println("image plane normal = " + v1);

            Vector3f correctZ = Vector3f.cross(v1, v2, null).normalise(null);
            System.out.println("cross product = " + correctZ);
            System.out.println("dot product = " + Vector3f.dot(v1, v2));

            result.set(axis.x * s, axis.y *s, axis.z * s, (float)Math.cos(angle/2.0));
            result.normalise(null);
        }
        else {
            result.set(0f, 0f, 0f, 1f); // Identity
        }   
        
        return result;
    }
}