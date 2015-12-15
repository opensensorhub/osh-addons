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


/**
 * <p>
 * Implementation of a 3-dimensional double precision vector object.<br/>
 * For efficiency, no checks for null pointers or NaN are done in this class.
 * </p>
 *
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Aug 9, 2015
 */
public class Vect3d
{
    public double x;
    public double y;
    public double z;
    
    
    /**
     * Creates a new zero vector
     */
    public Vect3d() {}
    
    
    /**
     * Creates a new vector from 3 scalar values
     * @param x X component value
     * @param y Y component value
     * @param z Z component value
     */
    public Vect3d(final double x, final double y, final double z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    
    /**
     * Creates a new vector using first 3 values of the array
     * @param a array of length >= 3
     */
    public Vect3d(final double[] a)
    {
        setFromArray(a);
    }
    
    
    /**
     * Sets values of all 3 vector components
     * @param x X component value
     * @param y Y component value
     * @param z Z component value
     * @return reference to this vector for chaining other operations
     */
    public Vect3d set(final double x, final double y, final double z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }
    
    
    /**
     * Sets this vector to be equal to another vector v
     * @param v the other vector
     * @return reference to this vector for chaining other operations
     */
    public final Vect3d set(final Vect3d v)
    {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        return this;
    }
    
    
    /**
     * Sets all components of this vector to zero
     * @return reference to this vector for chaining other operations
     */
    public final Vect3d setToZero()
    {
        this.x = this.y = this.z = 0.0;
        return this;
    }
    
    
    /**
     * Sets the value of this vector using first 3 values of the array
     * @param a array of length >= 3
     * @return reference to this vector for chaining other operations
     */
    public final Vect3d setFromArray(final double[] a)
    {
        this.x = a[0];
        this.y = a[1];
        this.z = a[2];
        return this;
    }
    
    
    /**
     * @return A fresh copy of this vector
     */
    public final Vect3d copy()
    {
        return new Vect3d(x, y, z);
    }
    
    
    /**
     * @return the Euclidean norm of this vector
     */
    public final double norm()
    {
        return Math.sqrt(normSq());
    }
    
    
    /**
     * @return the square of the Euclidean norm of this vector
     */
    public final double normSq()
    {
        return x*x + y*y + z*z;
    }
    
    
    /**
     * @return the uniform/inifinity norm of this vector
     */
    public final double normInf()
    {
        double tmp;
        tmp = Math.max(Math.abs(x), Math.abs(y));
        return Math.max(tmp, Math.abs(z));
    }
    
    
    /**
     * @return the L1 norm of this vector
     */
    public final double normL1()
    {
        return Math.abs(x) + Math.abs(y) + Math.abs(z);
    }
    
    
    /**
     * Normalizes this vector in place
     * @return reference to this vector for chaining other operations
     */
    public final Vect3d normalize()
    {
        double invNorm = 1.0 / norm();
        this.x *= invNorm;
        this.y *= invNorm;
        this.z *= invNorm;
        return this;
    }
    
    
    /**
     * Normalizes vector v and stores the result into this vector
     * @param v the other vector
     * @return reference to this vector for chaining other operations
     */
    public final Vect3d normalize(final Vect3d v)
    {
        double invNorm = 1.0 / v.norm();
        this.x = v.x * invNorm;
        this.y = v.y * invNorm;
        this.z = v.z * invNorm;
        return this;
    }
    
    
    /**
     * Computes this vector's euclidean norm and normalizes it in place
     * @return the vector's norm before normalization
     */
    public final double normalizeAndGetNorm()
    {
        double norm = norm();
        double invNorm = 1.0 / norm;
        this.x *= invNorm;
        this.y *= invNorm;
        this.z *= invNorm;
        return norm;
    }
    
    
    /**
     * Scales this vector by factor s
     * @param s the scale factor
     * @return reference to this vector for chaining other operations
     */
    public final Vect3d scale(final double s)
    {
        this.x *= s;
        this.y *= s;
        this.z *= s;
        return this;
    }
    
    
    /**
     * Scales vector v and stores the result into this vector
     * @param v the vector to be scaled
     * @param s the scale factor
     * @return reference to this vector for chaining other operations
     */
    public final Vect3d scale(final Vect3d v, final double s)
    {
        this.x = v.x * s;
        this.y = v.y * s;
        this.z = v.z * s;
        return this;
    }
    
    
    /**
     * Adds another vector to this vector
     * @param v other vector
     * @return reference to this vector for chaining other operations
     */
    public final Vect3d add(final Vect3d v)
    {
        this.x += v.x;
        this.y += v.y;
        this.z += v.z;
        return this;
    }
    
    
    /**
     * Adds vectors v1 and v2 and places the result in this vector
     * @param v1 first vector operand
     * @param v2 second vector operand
     * @return reference to this vector for chaining other operations 
     */
    public final Vect3d add(final Vect3d v1, final Vect3d v2)
    {
        this.x = v1.x + v2.x;
        this.y = v1.y + v2.y;
        this.z = v1.z + v2.z;
        return this;
    }
    
    
    /**
     * Subtracts another vector from this vector
     * @param v other vector
     * @return reference to this vector for chaining other operations
     */
    public final Vect3d sub(final Vect3d v)
    {
        this.x -= v.x;
        this.y -= v.y;
        this.z -= v.z;
        return this;
    }
    
    
    /**
     * Subtracts vectors v1 and v2 and places the result in this vector
     * @param v1 first vector operand
     * @param v2 second vector operand
     * @return reference to this vector for chaining other operations 
     */
    public final Vect3d sub(final Vect3d v1, final Vect3d v2)
    {
        this.x = v1.x - v2.x;
        this.y = v1.y - v2.y;
        this.z = v1.z - v2.z;
        return this;
    }
    
    
    /**
     * @param v other vector
     * @return The dot product of this vector with the other vector
     */
    public final double dot(final Vect3d v)
    {
        return this.x*v.x + this.y*v.y + this.z*v.z;
    }
    
    
    /**
     * Computes the cross product of this vector and v and places result in this vector
     * @param v the other vector
     * @return reference to this vector for chaining other operations
     */
    public final Vect3d cross(final Vect3d v)
    {
        double x = this.y * v.z - this.z * v.y;
        double y = this.z * v.x - this.x * v.z;
        this.z = this.x * v.y - this.y * v.x;
        this.x = x;
        this.y = y;
        return this;
    }
    
    
    /**
     * Computes the cross product of vectors v1 and v2 and places the result in this vector.<br/>
     * Note that this is not safe for aliasing (i.e. this cannot be v1 or v2). 
     * @param v1 first vector operand
     * @param v2 second vector operand
     * @return reference to this vector for chaining other operations 
     */
    public final Vect3d cross(final Vect3d v1, final Vect3d v2)
    {
        this.x = v1.y * v2.z - v1.z * v2.y;
        this.y = v1.z * v2.x - v1.x * v2.z;
        this.z = v1.x * v2.y - v1.y * v2.x;
        return this;
    }
    
    
    /**
     * Computes the separation angle between this vector and another vector.
     * Result is always within [0, PI]
     * @param v the other vector
     * @return separation angle
     */
    public final double separationAngle(final Vect3d v)
    {
        double vDot = this.dot(v) / ( this.norm() * v.norm());
        if (vDot < -1.0)
            vDot = -1.0;
        else if (vDot >  1.0)
            vDot = 1.0;
        return Math.acos(vDot);
    }
    
    
    /**
     * Transforms this vector using 3x3 matrix
     * @param m 3x3 matrix (e.g. rotation matrix)
     * @return reference to this vector for chaining other operations
     */
    public final Vect3d transform(final Mat3d m)
    {
        m.mul(this, this);
        return this;
    }
    
    
    /**
     * Transforms this vector using 4x4 matrix
     * @param m 4x4 matrix (e.g. homogeneous matrix)
     * @return reference to this vector for chaining other operations
     */
    public final Vect3d transform(final Mat4d m)
    {
        m.mul(this, this);
        return this;
    }
    
    
    /**
     * Rotates this vector using 3x3 matrix
     * @param m 3x3 rotation matrix
     * @return reference to this vector for chaining other operations
     */
    public final Vect3d rotate(final Mat3d m)
    {
        return transform(m);
    }
    
    
    /**
     * Rotates this vector using quaternion.<br/>
     * This actually computes the double quaternion product q.v.q'
     * @param q quaternion defining rotation
     * @return reference to this vector for chaining other operations
     */
    public final Vect3d rotate(final Quat4d q)
    {
        q.rotate(this, this);
        return this;
    }
    
    
    /**
     * Rotates vector around X axis by given angle
     * @param angle rotation angle in radians
     * @return reference to this vector for chaining other operations
     */
    public final Vect3d rotateX(final double angle)
    {
        double c, s;
        c = Math.cos(angle);
        s = Math.sin(angle);
        double y = c * this.y + -s * this.z;
        this.z = s * this.y + c * this.z;
        this.y = y;
        return this;
    }


    /**
     * Rotates this vector around Y axis by given angle
     * @param angle rotation angle in radians
     * @return reference to this vector for chaining other operations
     */
    public final Vect3d rotateY(final double angle)
    {
        double c, s;
        c = Math.cos(angle);
        s = Math.sin(angle);
        double x = c * this.x + s * this.z;
        this.z = -s * this.x + c * this.z;
        this.x = x;
        return this;
    }


    /**
     * Rotates this vector around Z axis by given angle
     * @param angle rotation angle in radians
     * @return reference to this vector for chaining other operations
     */
    public final Vect3d rotateZ(final double angle)
    {
        double c, s;
        c = Math.cos(angle);
        s = Math.sin(angle);
        double x = c * this.x - s * this.y;
        this.y = s * this.x + c * this.y;
        this.x = x;
        return this;
    }
    
    
    /**
     * Converts this vector to the 3 first components of the double array
     * @param a array of length >= 3
     */
    public final void toArray(double[] a)
    {
        a[0] = this.x;
        a[1] = this.y;
        a[2] = this.z;
    }
    
    
    @Override
    public final boolean equals(final Object o)
    {
        if (o == null || !(o instanceof Vect3d))
            return false;
        
        Vect3d v = (Vect3d)o;        
        if (v.x == this.x && v.y == this.y && v.z == this.z)
            return true;
        
        return false;
    }
    
    
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append('[')
           .append(x)
           .append(',')
           .append(y)
           .append(',')
           .append(z)
           .append(']');
        return buf.toString();
    }
}
