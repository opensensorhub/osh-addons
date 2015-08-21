/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.vecmath;

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
     * Creates a new (optionally identity) matrix
     * @param setToIdentity true to initialize it to identity
     */
    public Mat3d(boolean setToIdentity)
    {        
        if (setToIdentity)
            setIdentity();
    }
    
    
    /**
     * Creates a new matrix using values of given 2d array
     * @param a 3x3 array aranged in row-major order
     */
    public Mat3d(double[][] a)
    {        
        setFromArray2d(a, true);
    }
    
    
    /**
     * Creates a new matrix using values of given 2d array.
     * @see #setFromArray2d(double[][], boolean)
     * @param a 3x3 array
     * @param rowMajor true if array is in row-major order
     */
    public Mat3d(double[][] a, boolean rowMajor)
    {        
        setFromArray2d(a, rowMajor);
    }
    
    
    /**
     * Sets all components of this matrix to 0
     */
    public final void setZero()
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
    }
    
    
    /**
     * Sets this matrix to the identity matrix
     */
    public final void setIdentity()
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
    }
    
    
    /**
     * Sets the components of this matrix from the given array
     * @param a 3x3 array to read components from.
     * @param rowMajor true if array is in row-major order (e.g indexed as a[row][col]),
     * false if it is in column-major order
     */
    public final void setFromArray2d(double[][] a, boolean rowMajor)
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
    }
    
    
    /**
     * Sets the columns of this matrix from values of 3 vectors
     * @param c0 first column vector
     * @param c1 second column vector
     * @param c2 third column vector
     * @return reference to this matrix for chaining other operations
     */
    public Mat3d setCols(Vect3d c0, Vect3d c1, Vect3d c2)
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
     */
    public final void transpose()
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
    }
    
    
    /**
     * Adds a scalar to each component of this matrix
     * @param scalar
     */
    public final void add(double scalar)
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
    
    
    /**
     * Sets the value of this matrix to the sum of itself and matrix m.
     * @param m the other matrix
     */
    public final void add(Mat3d m)
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
    }
    
    
    /**
     * Multiplies all components of this matrix by a scalar
     * @param scalar
     */
    public final void mul(double scalar)
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
    }
    
    
    /**
     * Multiplies this matrix by a vector and stores the result in res.<br/>
     * Note that this is safe for aliasing (i.e. res can be v).
     * @param v vector
     * @param res vector to store the result into
     */
    public final void mul(Vect3d v, Vect3d res)
    {
        double x = m00 * v.x + m01*v.y + m02*v.z;
        double y = m10 * v.x + m11*v.y + m12*v.z;
        double z = m20 * v.x + m21*v.y + m22*v.z;
        
        res.x = x;
        res.y = y;
        res.z = z;
    }
    
    
    /**
     * Multiplies this matrix by another matrix and stores the result in res.<br/>
     * Note that this is safe for aliasing (i.e. res can be m or this matrix).
     * @param m other matrix
     * @param res matrix to store the result into
     */
    public final void mul(Mat3d m, Mat3d res)
    {
        double m00, m01, m02,
        m10, m11, m12,
        m20, m21, m22;

        m00 = this.m00 * m.m00 + this.m01 * m.m10 + this.m02 * m.m20;
        m01 = this.m00 * m.m01 + this.m01 * m.m11 + this.m02 * m.m21;
        m02 = this.m00 * m.m02 + this.m01 * m.m12 + this.m02 * m.m22;

        m10 = this.m10 * m.m00 + this.m11 * this.m10 + this.m12 * m.m20;
        m11 = this.m10 * m.m01 + this.m11 * this.m11 + this.m12 * m.m21;
        m12 = this.m10 * m.m02 + this.m11 * this.m12 + this.m12 * m.m22;

        m20 = this.m20 * m.m00 + this.m21 * m.m10 + this.m22 * m.m20;
        m21 = this.m20 * m.m01 + this.m21 * m.m11 + this.m22 * m.m21;
        m22 = this.m20 * m.m02 + this.m21 * m.m12 + this.m22 * m.m22;

        res.m00 = m00;
        res.m01 = m01;
        res.m02 = m02;
        res.m10 = m10;
        res.m11 = m11;
        res.m12 = m12;
        res.m20 = m20;
        res.m21 = m21;
        res.m22 = m22;
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
     * @return A quaternion representing the same rotation as this matrix
     */
    public final Quat4d toQuat()
    {
        Quat4d q = new Quat4d();
        toQuat(q);
        return q;
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
                q.w = Math.sqrt(ww);
                ww = 0.25 / q.w;
                q.x = (this.m21 - this.m12) * ww;
                q.y = (this.m02 - this.m20) * ww;
                q.z = (this.m10 - this.m01) * ww;
                return;
            }
        }
        else
        {
            q.w = 0;
            q.x = 0;
            q.y = 0;
            q.z = 1;
            return;
        }

        q.w = 0;
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
}
