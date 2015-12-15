/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.algo.vecmath;

import java.io.Serializable;

/**
 * <p>
 * Implementation of a 4x4 double precision matrix object.<br/>
 * For efficiency, no checks for null pointers or NaN are done in this class.
 * </p>
 *
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Aug 9, 2015
 */
public class Mat4d implements Serializable
{
    private static final long serialVersionUID = -5068101389336623092L;
    final static double EPS = 1.0e-12;
    final static double EPS2 = 1.0e-30;    
    
    public double m00, m01, m02, m03,
                  m10, m11, m12, m13,
                  m20, m21, m22, m23,
                  m30, m31, m32, m33;


    /**
     * Creates a zero matrix
     */
    public Mat4d()
    {
    }
    
    
    /**
     * Creates a new matrix using values of given 2d array
     * @param a 3x3 array aranged in row-major order
     */
    public Mat4d(final double[][] a)
    {        
        setFromArray2d(a, true);
    }
    
    
    /**
     * Creates a new matrix using values of given 2d array.
     * @see #setFromArray2d(double[][], boolean)
     * @param a 3x3 array
     * @param rowMajor true if array is in row-major order
     */
    public Mat4d(final double[][] a, final boolean rowMajor)
    {        
        setFromArray2d(a, rowMajor);
    }
    
    
    /**
     * @return A fresh copy of this matrix
     */
    public final Mat4d copy()
    {
        Mat4d m = new Mat4d();
        
        m.m00 = this.m00;
        m.m01 = this.m01;
        m.m02 = this.m02;
        m.m03 = this.m03;
        
        m.m10 = this.m10;
        m.m11 = this.m11;
        m.m12 = this.m12;
        m.m13 = this.m13;
        
        m.m20 = this.m20;
        m.m21 = this.m21;
        m.m22 = this.m22;
        m.m23 = this.m23;
        
        m.m30 = this.m30;
        m.m31 = this.m31;
        m.m32 = this.m32;
        m.m33 = this.m33;
        
        return m;
    }
    
    
    /**
     * Sets all components of this matrix to 0
     * @return reference to this matrix for chaining other operations
     */
    public final Mat4d setZero()
    {
        this.m00 = 0.0;
        this.m01 = 0.0;
        this.m02 = 0.0;
        this.m03 = 0.0;
        
        this.m10 = 0.0;
        this.m11 = 0.0;
        this.m12 = 0.0;
        this.m13 = 0.0;
        
        this.m20 = 0.0;
        this.m21 = 0.0;
        this.m22 = 0.0;
        this.m23 = 0.0;
        
        this.m30 = 0.0;
        this.m31 = 0.0;
        this.m32 = 0.0;
        this.m33 = 0.0;
        
        return this;
    }
    
    
    /**
     * Sets this matrix to the identity matrix
     * @return reference to this matrix for chaining other operations
     */
    public final Mat4d setIdentity()
    {
        this.m00 = 1.0;
        this.m01 = 0.0;
        this.m02 = 0.0;    
        this.m03 = 0.0;
        
        this.m10 = 0.0;
        this.m11 = 1.0;
        this.m12 = 0.0;    
        this.m13 = 0.0;
        
        this.m20 = 0.0;
        this.m21 = 0.0;
        this.m22 = 1.0;
        this.m23 = 0.0;
        
        this.m30 = 0.0;
        this.m31 = 0.0;
        this.m32 = 0.0;
        this.m33 = 1.0;
        
        return this;
    }
    
    
    /**
     * Sets the components of this matrix from the given array
     * @param a 4x4 array to read components from.
     * @param rowMajor true if array is in row-major order (e.g indexed as a[row][col]),
     * false if it is in column-major order
     * @return reference to this matrix for chaining other operations
     */
    public final Mat4d setFromArray2d(final double[][] a, final boolean rowMajor)
    {
        if (rowMajor)
        {
            this.m00 = a[0][0];
            this.m01 = a[0][1];
            this.m02 = a[0][2];    
            this.m03 = a[0][3];
            
            this.m10 = a[1][0];
            this.m11 = a[1][1];
            this.m12 = a[1][2];    
            this.m13 = a[1][3]; 
            
            this.m20 = a[2][0];
            this.m21 = a[2][1];
            this.m22 = a[2][2];
            this.m23 = a[2][3];
            
            this.m30 = a[3][0];
            this.m31 = a[3][1];
            this.m32 = a[3][2];
            this.m33 = a[3][3];
        }
        else
        {
            this.m00 = a[0][0];
            this.m01 = a[1][0];
            this.m02 = a[2][0]; 
            this.m03 = a[3][0];
            
            this.m10 = a[0][1];
            this.m11 = a[1][1];
            this.m12 = a[2][1];
            this.m13 = a[3][1];
            
            this.m20 = a[0][2];
            this.m21 = a[1][2];
            this.m22 = a[2][2];
            this.m23 = a[3][2];
            
            this.m30 = a[0][3];
            this.m31 = a[1][3];
            this.m32 = a[2][3];
            this.m33 = a[3][3];
        }
        
        return this;
    }
    
    
    /**
     * Sets this matrix to be equal to another matrix
     * @param m other 4x4 matrix
     * @return reference to this matrix for chaining other operations
     */
    public final Mat4d set(Mat4d m)
    {
        this.m00 = m.m00;
        this.m01 = m.m01;
        this.m02 = m.m02;
        this.m03 = m.m03;
        
        this.m10 = m.m10;
        this.m11 = m.m11;
        this.m12 = m.m12;
        this.m13 = m.m13;
        
        this.m20 = m.m20;
        this.m21 = m.m21;
        this.m22 = m.m22;
        this.m23 = m.m23;
        
        this.m30 = m.m30;
        this.m31 = m.m31;
        this.m32 = m.m32;
        this.m33 = m.m33;
        
        return this;
    }
    
    
    /**
     * Sets the top left part of this matrix to be equal to the given 3x3 matrix
     * @param m other 3x3 matrix
     * @return reference to this matrix for chaining other operations
     */
    public final Mat4d set(Mat3d m)
    {
        this.m00 = m.m00;
        this.m01 = m.m01;
        this.m02 = m.m02;
        
        this.m10 = m.m10;
        this.m11 = m.m11;
        this.m12 = m.m12;
        
        this.m20 = m.m20;
        this.m21 = m.m21;
        this.m22 = m.m22;
        
        return this;
    }
    
    
    /**
     * Sets the columns of this matrix from values of 3 3D vectors.<br/>
     * Only the first 3 rows are set and the scale factor (m33) is set to 1.0.
     * @param c0 first column vector
     * @param c1 second column vector
     * @param c2 third column vector
     * @param t fourth column vector = translation vector
     * @return reference to this matrix for chaining other operations
     */
    public final Mat4d setCols(final Vect3d c0, final Vect3d c1, final Vect3d c2, final Vect3d t)
    {
        this.m00 = c0.x;
        this.m10 = c0.y;
        this.m20 = c0.z;
        
        this.m01 = c1.x;
        this.m11 = c1.y;
        this.m21 = c1.z;
        
        this.m02 = c2.x;    
        this.m12 = c2.y;    
        this.m22 = c2.z;
        
        this.m03 = t.x;    
        this.m13 = t.y;    
        this.m23 = t.z;
        
        this.m33 = 1.0;
        
        return this;
    }
    
    
    /**
     * Sets the translation part of this matrix
     * @param t translation vector
     * @return reference to this matrix for chaining other operations
     */
    public final Mat4d setTranslation(final Vect3d t)
    {
        this.m03 = t.x;
        this.m13 = t.y;
        this.m23 = t.z;
        return this;
    }
    
    
    /**
     * Sets the translation part of this matrix
     * @param tx
     * @param ty
     * @param tz
     * @return reference to this matrix for chaining other operations
     */
    public final Mat4d setTranslation(final double tx, final double ty, final double tz)
    {
        this.m03 = tx;
        this.m13 = ty;
        this.m23 = tz;
        return this;
    }
    
    
    /**
     * Sets the value of this matrix to its transpose.
     * @return reference to this matrix for chaining other operations
     */
    public final Mat4d transpose()
    {
        double temp;
    
        temp = this.m10;
        this.m10 = this.m01;
        this.m01 = temp;
    
        temp = this.m20;
        this.m20 = this.m02;
        this.m02 = temp;
        
        temp = this.m30;
        this.m30 = this.m03;
        this.m03 = temp;
    
        temp = this.m21;
        this.m21 = this.m12;
        this.m12 = temp;
        
        temp = this.m31;
        this.m31 = this.m13;
        this.m13 = temp;
        
        temp = this.m32;
        this.m32 = this.m23;
        this.m23 = temp;
        
        return this;
    }
    
    
    /**
     * @return the trace of this matrix
     */
    public final double trace()
    {
        return m00 + m11 + m22 + m33;
    }
    
    
    /**
     * @return the determinant of this matrix
     */
    public final double determinant()
    {
       return m00 * (m11 * m22 - m12 * m21) +
              m01 * (m12 * m20 - m10 * m22) +
              m02 * (m10 * m21 - m11 * m20);
    }
    
    
    /**
     * Adds a scalar to each component of this matrix
     * @param scalar
     * @return reference to this matrix for chaining other operations
     */
    public final Mat4d add(final double scalar)
    {
        if (scalar != 0)
        {
            this.m00 += scalar;
            this.m01 += scalar;
            this.m02 += scalar;
            this.m03 += scalar;
            
            this.m10 += scalar;
            this.m11 += scalar;
            this.m12 += scalar;
            this.m13 += scalar;
            
            this.m20 += scalar;
            this.m21 += scalar;
            this.m22 += scalar;
            this.m23 += scalar;
            
            this.m30 += scalar;
            this.m31 += scalar;
            this.m32 += scalar;
            this.m33 += scalar;
        }
        
        return this;
    }
    
    
    /**
     * Sets the value of this matrix to the sum of itself and matrix m.
     * @param m the other matrix
     * @return reference to this matrix for chaining other operations
     */
    public final Mat4d add(final Mat4d m)
    {
        this.m00 += m.m00;
        this.m01 += m.m01;
        this.m02 += m.m02;
        this.m03 += m.m03;
        
        this.m10 += m.m10;
        this.m11 += m.m11;
        this.m12 += m.m12;
        this.m13 += m.m13;
        
        this.m20 += m.m20;
        this.m21 += m.m21;
        this.m22 += m.m22;
        this.m23 += m.m23;
        
        this.m30 += m.m30;
        this.m31 += m.m31;
        this.m32 += m.m32;
        this.m33 += m.m33;
        
        return this;
    }
    
    
    /**
     * Multiplies all components of this matrix by a scalar
     * @param scalar
     * @return reference to this matrix for chaining other operations
     */
    public final Mat4d mul(final double scalar)
    {
        this.m00 *= scalar;
        this.m01 *= scalar;
        this.m02 *= scalar;
        this.m03 *= scalar;
        
        this.m10 *= scalar;
        this.m11 *= scalar;
        this.m12 *= scalar;
        this.m13 *= scalar;
        
        this.m20 *= scalar;
        this.m21 *= scalar;
        this.m22 *= scalar;
        this.m23 *= scalar;
        
        this.m30 *= scalar;
        this.m31 *= scalar;
        this.m32 *= scalar;
        this.m33 *= scalar;
        
        return this;
    }
    
    
    /**
     * Multiplies this matrix by another matrix and places the result in this matrix.<br/>
     * @param m other matrix
     * @return reference to this matrix for chaining other operations
     */
    public final Mat4d mul(final Mat4d m)
    {
        double m00, m01, m02, m03,
        m10, m11, m12, m13,
        m20, m21, m22, m23,
        m30, m31, m32, m33;

        m00 = this.m00 * m.m00 + this.m01 * m.m10 + this.m02 * m.m20 + this.m03 * m.m30;
        m01 = this.m00 * m.m01 + this.m01 * m.m11 + this.m02 * m.m21 + this.m03 * m.m31;
        m02 = this.m00 * m.m02 + this.m01 * m.m12 + this.m02 * m.m22 + this.m03 * m.m32;
        m03 = this.m00 * m.m03 + this.m01 * m.m13 + this.m02 * m.m23 + this.m03 * m.m33;

        m10 = this.m10 * m.m00 + this.m11 * m.m10 + this.m12 * m.m20 + this.m13 * m.m30;
        m11 = this.m10 * m.m01 + this.m11 * m.m11 + this.m12 * m.m21 + this.m13 * m.m31;
        m12 = this.m10 * m.m02 + this.m11 * m.m12 + this.m12 * m.m22 + this.m13 * m.m32;
        m13 = this.m10 * m.m03 + this.m11 * m.m13 + this.m12 * m.m23 + this.m13 * m.m33;

        m20 = this.m20 * m.m00 + this.m21 * m.m10 + this.m22 * m.m20 + this.m23 * m.m30;
        m21 = this.m20 * m.m01 + this.m21 * m.m11 + this.m22 * m.m21 + this.m23 * m.m31;
        m22 = this.m20 * m.m02 + this.m21 * m.m12 + this.m22 * m.m22 + this.m23 * m.m32;
        m23 = this.m20 * m.m03 + this.m21 * m.m13 + this.m22 * m.m23 + this.m23 * m.m33;
        
        m30 = this.m30 * m.m00 + this.m31 * m.m10 + this.m32 * m.m20 + this.m33 * m.m30;
        m31 = this.m30 * m.m01 + this.m31 * m.m11 + this.m32 * m.m21 + this.m33 * m.m31;
        m32 = this.m30 * m.m02 + this.m31 * m.m12 + this.m32 * m.m22 + this.m33 * m.m32;
        m33 = this.m30 * m.m03 + this.m31 * m.m13 + this.m32 * m.m23 + this.m33 * m.m33;

        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m03 = m03;
        
        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m13 = m13;
        
        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
        this.m23 = m23;
        
        this.m30 = m30;
        this.m31 = m31;
        this.m32 = m32;
        this.m33 = m33;
        
        return this;
    }
    
    
    /**
     * Multiplies matrices m1 and m2 and places the result in this matrix.<br/>
     * Note that this is NOT safe for aliasing (i.e. this cannot be m1 or m2).
     * @param m1 left matrix
     * @param m2 right matrix
     * @return reference to this matrix for chaining other operations
     */
    public final Mat4d mul(final Mat4d m1, final Mat4d m2)
    {
        this.m00 = m1.m00 * m2.m00 + m1.m01 * m2.m10 + m1.m02 * m2.m20 + m1.m03 * m2.m30;
        this.m01 = m1.m00 * m2.m01 + m1.m01 * m2.m11 + m1.m02 * m2.m21 + m1.m03 * m2.m31;
        this.m02 = m1.m00 * m2.m02 + m1.m01 * m2.m12 + m1.m02 * m2.m22 + m1.m03 * m2.m32;
        this.m03 = m1.m00 * m2.m03 + m1.m01 * m2.m13 + m1.m02 * m2.m23 + m1.m03 * m2.m33;

        this.m10 = m1.m10 * m2.m00 + m1.m11 * m2.m10 + m1.m12 * m2.m20 + m1.m13 * m2.m30;
        this.m11 = m1.m10 * m2.m01 + m1.m11 * m2.m11 + m1.m12 * m2.m21 + m1.m13 * m2.m31;
        this.m12 = m1.m10 * m2.m02 + m1.m11 * m2.m12 + m1.m12 * m2.m22 + m1.m13 * m2.m32;
        this.m13 = m1.m10 * m2.m03 + m1.m11 * m2.m13 + m1.m12 * m2.m23 + m1.m13 * m2.m33;

        this.m20 = m1.m20 * m2.m00 + m1.m21 * m2.m10 + m1.m22 * m2.m20 + m1.m23 * m2.m30;
        this.m21 = m1.m20 * m2.m01 + m1.m21 * m2.m11 + m1.m22 * m2.m21 + m1.m23 * m2.m31;
        this.m22 = m1.m20 * m2.m02 + m1.m21 * m2.m12 + m1.m22 * m2.m22 + m1.m23 * m2.m32;
        this.m23 = m1.m20 * m2.m03 + m1.m21 * m2.m13 + m1.m22 * m2.m23 + m1.m23 * m2.m33;
        
        this.m30 = m1.m30 * m2.m00 + m1.m31 * m2.m10 + m1.m32 * m2.m20 + m1.m33 * m2.m30;
        this.m31 = m1.m30 * m2.m01 + m1.m31 * m2.m11 + m1.m32 * m2.m21 + m1.m33 * m2.m31;
        this.m32 = m1.m30 * m2.m02 + m1.m31 * m2.m12 + m1.m32 * m2.m22 + m1.m33 * m2.m32;
        this.m33 = m1.m30 * m2.m03 + m1.m31 * m2.m13 + m1.m32 * m2.m23 + m1.m33 * m2.m33;
        
        return this;
    }
    
    
    /**
     * Rotate the 3x3 part of this matrix about x-axis (right-handed rotation)
     * @param angleRadians
     * @return reference to this matrix for chaining other operations
     */
    public final Mat4d rotateX(final double angleRadians)
    {
        if (angleRadians != 0)
        {
            double c, s, m;
            c = Math.cos(angleRadians);
            s = Math.sin(angleRadians);
    
            m = m10;
            m10 = s * m20 + c * m;
            m20 = c * m20 - s * m;
            
            m = m11;
            m11 = s * m21 + c * m;
            m21 = c * m21 - s * m;
            
            m = m12;
            m12 = s * m22 + c * m;
            m22 = c * m22 - s * m;
        }
        
        return this;
    }


    /**
     * Rotate the 3x3 part of this matrix about y-axis (right-handed rotation)
     * @param angleRadians
     * @return reference to this matrix for chaining other operations
     */
    public final Mat4d rotateY(final double angleRadians)
    {
        if (angleRadians != 0)
        {
            double c, s, m;
            c = Math.cos(angleRadians);
            s = Math.sin(angleRadians);
    
            m = m00;
            m00 = s * m20 + c * m;
            m20 = c * m20 - s * m;
            
            m = m01;
            m01 = s * m21 + c * m;
            m21 = c * m21 - s * m;
            
            m = m02;
            m02 = s * m22 + c * m;
            m22 = c * m22 - s * m;
        }
        
        return this;
    }


    /**
     * Rotate the 3x3 part of this matrix about z-axis (right-handed rotation)
     * @param angleRadians
     * @return reference to this matrix for chaining other operations
     */
    public final Mat4d rotateZ(final double angleRadians)
    {
        if (angleRadians != 0)
        {
            double c, s, m;
            c = Math.cos(angleRadians);
            s = Math.sin(angleRadians);
    
            m = m00;
            m00 = s * m10 + c * m;
            m10 = c * m10 - s * m;
            
            m = m01;
            m01 = s * m11 + c * m;
            m11 = c * m11 - s * m;
            
            m = m02;
            m02 = s * m12 + c * m;
            m12 = c * m12 - s * m;
        }
        
        return this;
    }
    
    
    public final double getElement(final int i, final int j)
    {
        if (i == 0)
        {
            if (j == 0)
                return m00;
            else if (j == 1)
                return m01;
            else if (j == 2)
                return m02;
            else if (j == 3)
                return m03;
        }
        else if (i == 1)
        {
            if (j == 0)
                return m10;
            else if (j == 1)
                return m11;
            else if (j == 2)
                return m12;
            else if (j == 3)
                return m13;
        }
        else if (i == 2)
        {
            if (j == 0)
                return m20;
            else if (j == 1)
                return m21;
            else if (j == 2)
                return m22;
            else if (j == 3)
                return m23;
        }
        else if (i == 3)
        {
            if (j == 0)
                return m30;
            else if (j == 1)
                return m31;
            else if (j == 2)
                return m32;
            else if (j == 3)
                return m33;
        }
        
        throw new IndexOutOfBoundsException();
    }
    
    
    public final void setElement(final int i, final int j, double val)
    {
        if (i < 0 || i > 3 || j < 0 || j > 3)
            throw new IndexOutOfBoundsException();
        
        if (i == 0)
        {
            if (j == 0)
                m00 = val;
            else if (j == 1)
                m01 = val;
            else if (j == 2)
                m02 = val;
            else if (j == 3)
                m03 = val;
        }
        else if (i == 1)
        {
            if (j == 0)
                m10 = val;
            else if (j == 1)
                m11 = val;
            else if (j == 2)
                m12 = val;
            else if (j == 3)
                m13 = val;
        }
        else if (i == 2)
        {
            if (j == 0)
                m20 = val;
            else if (j == 1)
                m21 = val;
            else if (j == 2)
                m22 = val;
            else if (j == 3)
                m23 = val;
        }
        else if (i == 3)
        {
            if (j == 0)
                m30 = val;
            else if (j == 1)
                m31 = val;
            else if (j == 2)
                m32 = val;
            else if (j == 3)
                m33 = val;
        }
    }
    
    
    /**
     * Multiplies the first three row of this matrix by a 3D vector and
     * stores the result in res.<br/>
     * Note that this is safe for aliasing (i.e. res can be v).
     * @param v vector
     * @param res vector to store the result into
     */
    public final void mul(final Vect3d v, final Vect3d res)
    {
        double x = m00 * v.x + m01*v.y + m02*v.z + m03;
        double y = m10 * v.x + m11*v.y + m12*v.z + m13;
        double z = m20 * v.x + m21*v.y + m22*v.z + m23;
        
        res.x = x;
        res.y = y;
        res.z = z;
    }
    
    
    /**
     * @return A quaternion representing the same rotation as this matrix
     */
    public final Quat4d toQuat()
    {
        Quat4d q = new Quat4d();
        toQuat(q);
        return q;
    }
    
    
    /**
     * Converts the 3x3 part (rotation) of this matrix to a quaternion.
     * @param q Quaternion object to receive the result
     */
    public final void toQuat(final Quat4d q)
    {
        double ww = 0.25 * (this.m00 + this.m11 + this.m22 + 1.0);

        if (ww >= 0)
        {
            if (ww >= EPS2)
            {
                q.s = Math.sqrt(ww);
                ww = 0.25 / q.s;
                q.x = (this.m21 - this.m12) * ww;
                q.y = (this.m02 - this.m20) * ww;
                q.z = (this.m10 - this.m01) * ww;
                return;
            }
        }
        else
        {
            q.s = 0;
            q.x = 0;
            q.y = 0;
            q.z = 1;
            return;
        }

        q.s = 0;
        ww = -0.5 * (this.m11 + this.m22);
        if (ww >= 0)
        {
            if (ww >= EPS2)
            {
                q.x = Math.sqrt(ww);
                ww = 0.5 / q.x;
                q.y = this.m10 * ww;
                q.z = this.m20 * ww;
                return;
            }
        }
        else
        {
            q.x = 0;
            q.y = 0;
            q.z = 1;
            return;
        }

        q.x = 0;
        ww = 0.5 * (1.0 - this.m22);
        if (ww >= EPS2)
        {
            q.y = Math.sqrt(ww);
            q.z = this.m21 / (2.0 * q.y);
            return;
        }

        q.y = 0;
        q.z = 1;
    }
    
    
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append('[')
           .append(m00).append(',').append(m01).append(',').append(m02).append(',').append(m03).append('\n')
           .append(m10).append(',').append(m11).append(',').append(m12).append(',').append(m13).append('\n')
           .append(m20).append(',').append(m21).append(',').append(m22).append(',').append(m23).append('\n')
           .append(m30).append(',').append(m31).append(',').append(m32).append(',').append(m33).append(']');
        return buf.toString();
    }
}
