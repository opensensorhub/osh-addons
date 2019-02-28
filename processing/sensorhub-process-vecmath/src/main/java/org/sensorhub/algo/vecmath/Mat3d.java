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
 * Implementation of a 3x3 double precision matrix object.<br/>
 * For efficiency, no checks for null pointers or NaN are done in this class.
 * </p>
 *
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Aug 9, 2015
 */
public class Mat3d implements Serializable
{
    private static final long serialVersionUID = -5068101389336623092L;
    final static double EPS = 1.0e-12;
    final static double EPS2 = 1.0e-30;    
    
    public double m00, m01, m02, m10, m11, m12, m20, m21, m22; 


    /**
     * Creates a zero matrix
     */
    public Mat3d()
    {
    }
    
    
    /**
     * Creates a new matrix using values of given 2d array
     * @param a 3x3 array aranged in row-major order
     */
    public Mat3d(final double[][] a)
    {        
        setFromArray2d(a, true);
    }
    
    
    /**
     * Creates a new matrix using values of given 2d array.
     * @see #setFromArray2d(double[][], boolean)
     * @param a 3x3 array
     * @param rowMajor true if array is in row-major order
     */
    public Mat3d(final double[][] a, final boolean rowMajor)
    {        
        setFromArray2d(a, rowMajor);
    }
    
    
    /**
     * @return A fresh copy of this matrix
     */
    public final Mat3d copy()
    {
        Mat3d m = new Mat3d();
        m.m00 = this.m00;
        m.m01 = this.m01;
        m.m02 = this.m02;
        m.m10 = this.m10;
        m.m11 = this.m11;
        m.m12 = this.m12;
        m.m20 = this.m20;
        m.m21 = this.m21;
        m.m22 = this.m22;
        return m;
    }
    
    
    /**
     * Sets all components of this matrix to 0
     * @return reference to this matrix for chaining other operations
     */
    public final Mat3d setZero()
    {
        this.m00 = 0.0;
        this.m01 = 0.0;
        this.m02 = 0.0;    
        this.m10 = 0.0;
        this.m11 = 0.0;
        this.m12 = 0.0;    
        this.m20 = 0.0;
        this.m21 = 0.0;
        this.m22 = 0.0;
        return this;
    }
    
    
    /**
     * Sets this matrix to the identity matrix
     * @return reference to this matrix for chaining other operations
     */
    public final Mat3d setIdentity()
    {
        this.m00 = 1.0;
        this.m01 = 0.0;
        this.m02 = 0.0;    
        this.m10 = 0.0;
        this.m11 = 1.0;
        this.m12 = 0.0;    
        this.m20 = 0.0;
        this.m21 = 0.0;
        this.m22 = 1.0;
        return this;
    }
    
    
    /**
     * Sets the components of this matrix from the given array
     * @param a 3x3 array to read components from.
     * @param rowMajor true if array is in row-major order (e.g indexed as a[row][col]),
     * false if it is in column-major order
     * @return reference to this matrix for chaining other operations
     */
    public final Mat3d setFromArray2d(final double[][] a, final boolean rowMajor)
    {
        if (rowMajor)
        {
            this.m00 = a[0][0];
            this.m01 = a[0][1];
            this.m02 = a[0][2];    
            this.m10 = a[1][0];
            this.m11 = a[1][1];
            this.m12 = a[1][2];    
            this.m20 = a[2][0];
            this.m21 = a[2][1];
            this.m22 = a[2][2];
        }
        else
        {
            this.m00 = a[0][0];
            this.m01 = a[1][0];
            this.m02 = a[2][0];    
            this.m10 = a[0][1];
            this.m11 = a[1][1];
            this.m12 = a[2][1];    
            this.m20 = a[0][2];
            this.m21 = a[1][2];
            this.m22 = a[2][2];
        }
        
        return this;
    }
    
    
    /**
     * Sets this matrix to be equal to another 3x3 matrix
     * @param m other 3x3 matrix
     * @return reference to this matrix for chaining other operations
     */
    public final Mat3d set(Mat3d m)
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
     * Sets the columns of this matrix from values of 3 vectors
     * @param c0 first column vector
     * @param c1 second column vector
     * @param c2 third column vector
     * @return reference to this matrix for chaining other operations
     */
    public Mat3d setCols(final Vect3d c0, final Vect3d c1, final Vect3d c2)
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
        
        return this;
    }
    
    
    /**
     * Sets the value of this matrix to its transpose.
     * @return reference to this matrix for chaining other operations
     */
    public final Mat3d transpose()
    {
        double temp;
    
        temp = this.m10;
        this.m10 = this.m01;
        this.m01 = temp;
    
        temp = this.m20;
        this.m20 = this.m02;
        this.m02 = temp;
    
        temp = this.m21;
        this.m21 = this.m12;
        this.m12 = temp;
        
        return this;
    }
    
    
    /**
     * @return the trace of this matrix
     */
    public final double trace()
    {
        return m00 + m11 + m22;
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
    public final Mat3d add(final double scalar)
    {
        if (scalar != 0)
        {
            this.m00 += scalar;
            this.m01 += scalar;
            this.m02 += scalar;
            
            this.m10 += scalar;
            this.m11 += scalar;
            this.m12 += scalar;
            
            this.m20 += scalar;
            this.m21 += scalar;
            this.m22 += scalar;
        }
        
        return this;
    }
    
    
    /**
     * Sets the value of this matrix to the sum of itself and matrix m.
     * @param m the other matrix
     * @return reference to this matrix for chaining other operations
     */
    public final Mat3d add(final Mat3d m)
    {
        this.m00 += m.m00;
        this.m01 += m.m01;
        this.m02 += m.m02;

        this.m10 += m.m10;
        this.m11 += m.m11;
        this.m12 += m.m12;

        this.m20 += m.m20;
        this.m21 += m.m21;
        this.m22 += m.m22;
        
        return this;
    }
    
    
    /**
     * Multiplies all components of this matrix by a scalar
     * @param scalar
     * @return reference to this matrix for chaining other operations
     */
    public final Mat3d mul(final double scalar)
    {
        this.m00 *= scalar;
        this.m01 *= scalar;
        this.m02 *= scalar;
        
        this.m10 *= scalar;
        this.m11 *= scalar;
        this.m12 *= scalar;
        
        this.m20 *= scalar;
        this.m21 *= scalar;
        this.m22 *= scalar;
        
        return this;
    }
    
    
    /**
     * Multiplies this matrix by another matrix m and places the result in this matrix.<br/>
     * Note that this is NOT safe for aliasing (i.e. this cannot be m1 or m2).
     * @param m left matrix
     * @return reference to this matrix for chaining other operations
     */
    public final Mat3d mul(final Mat3d m)
    {
        double m00, m01, m02,
        m10, m11, m12,
        m20, m21, m22;

        m00 = this.m00 * m.m00 + this.m01 * m.m10 + this.m02 * m.m20;
        m01 = this.m00 * m.m01 + this.m01 * m.m11 + this.m02 * m.m21;
        m02 = this.m00 * m.m02 + this.m01 * m.m12 + this.m02 * m.m22;

        m10 = this.m10 * m.m00 + this.m11 * m.m10 + this.m12 * m.m20;
        m11 = this.m10 * m.m01 + this.m11 * m.m11 + this.m12 * m.m21;
        m12 = this.m10 * m.m02 + this.m11 * m.m12 + this.m12 * m.m22;

        m20 = this.m20 * m.m00 + this.m21 * m.m10 + this.m22 * m.m20;
        m21 = this.m20 * m.m01 + this.m21 * m.m11 + this.m22 * m.m21;
        m22 = this.m20 * m.m02 + this.m21 * m.m12 + this.m22 * m.m22;

        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
        
        return this;
    }
    
    
    /**
     * Multiplies matrices m1 and m2 and places the result in this matrix.<br/>
     * Note that this is NOT safe for aliasing (i.e. this cannot be m1 or m2).
     * @param m1 left matrix
     * @param m2 right matrix
     * @return reference to this matrix for chaining other operations
     */
    public final Mat3d mul(final Mat3d m1, final Mat3d m2)
    {
        this.m00 = m1.m00 * m2.m00 + m1.m01 * m2.m10 + m1.m02 * m2.m20;
        this.m01 = m1.m00 * m2.m01 + m1.m01 * m2.m11 + m1.m02 * m2.m21;
        this.m02 = m1.m00 * m2.m02 + m1.m01 * m2.m12 + m1.m02 * m2.m22;

        this.m10 = m1.m10 * m2.m00 + m1.m11 * m2.m10 + m1.m12 * m2.m20;
        this.m11 = m1.m10 * m2.m01 + m1.m11 * m2.m11 + m1.m12 * m2.m21;
        this.m12 = m1.m10 * m2.m02 + m1.m11 * m2.m12 + m1.m12 * m2.m22;

        this.m20 = m1.m20 * m2.m00 + m1.m21 * m2.m10 + m1.m22 * m2.m20;
        this.m21 = m1.m20 * m2.m01 + m1.m21 * m2.m11 + m1.m22 * m2.m21;
        this.m22 = m1.m20 * m2.m02 + m1.m21 * m2.m12 + m1.m22 * m2.m22;
        
        return this;
    } 
    
    
    /**
     * Rotate this matrix about x-axis (right-handed rotation)
     * @param angleRadians
     * @return reference to this matrix for chaining other operations
     */
    public final Mat3d rotateX(final double angleRadians)
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
     * Rotate this matrix about y-axis (right-handed rotation)
     * @param angleRadians
     * @return reference to this matrix for chaining other operations
     */
    public final Mat3d rotateY(final double angleRadians)
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
     * Rotate this matrix about z-axis (right-handed rotation)
     * @param angleRadians
     * @return reference to this matrix for chaining other operations
     */
    public final Mat3d rotateZ(final double angleRadians)
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
        }
        else if (i == 1)
        {
            if (j == 0)
                return m10;
            else if (j == 1)
                return m11;
            else if (j == 2)
                return m12;
        }
        else if (i == 2)
        {
            if (j == 0)
                return m20;
            else if (j == 1)
                return m21;
            else if (j == 2)
                return m22;
        }
        
        throw new IndexOutOfBoundsException();
    }
    
    
    public final void setElement(final int i, final int j, final double val)
    {
        if (i < 0 || i > 2 || j < 0 || j > 2)
            throw new IndexOutOfBoundsException();
        
        if (i == 0)
        {
            if (j == 0)
                m00 = val;
            else if (j == 1)
                m01 = val;
            else if (j == 2)
                m02 = val;
        }
        else if (i == 1)
        {
            if (j == 0)
                m10 = val;
            else if (j == 1)
                m11 = val;
            else if (j == 2)
                m12 = val;
        }
        else if (i == 2)
        {
            if (j == 0)
                m20 = val;
            else if (j == 1)
                m21 = val;
            else if (j == 2)
                m22 = val;
        }
    }
    
    
    /**
     * Multiplies this matrix by a vector and stores the result in res.<br/>
     * Note that this is safe for aliasing (i.e. res can be v).
     * @param v vector
     * @param res vector to store the result into
     */
    public final void mul(final Vect3d v, final Vect3d res)
    {
        double x = m00 * v.x + m01*v.y + m02*v.z;
        double y = m10 * v.x + m11*v.y + m12*v.z;
        double z = m20 * v.x + m21*v.y + m22*v.z;
        
        res.x = x;
        res.y = y;
        res.z = z;
    }
    
    
    /**
     * Converts this matrix to a quaternion.
     * @param q Quaternion object to receive the result
     */
    public final void toQuat(Quat4d q)
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
           .append(m00).append(',').append(m01).append(',').append(m02).append('\n')
           .append(m10).append(',').append(m11).append(',').append(m12).append('\n')
           .append(m20).append(',').append(m21).append(',').append(m22).append(']');
        return buf.toString();
    }
}
