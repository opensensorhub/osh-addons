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
 * Implementation of a double precision quaternion.<br/>
 * For efficiency, no checks for null pointers or NaN are done in this class.
 * </p>
 *
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Aug 9, 2015
 */
public class Quat4d implements Serializable
{
    private static final long serialVersionUID = -8715785756091976649L;
    
    public double x;
    public double y;
    public double z;
    public double s; // scalar component
    

    /**
     * Creates a new quaternion initialized to identity 
     */
    public Quat4d()
    {
        setToIdentity();
    }


    /**
     * Creates a quaternion from scalar values for the 4 components
     * @param qx quaternion x component
     * @param qy quaternion y component
     * @param qz quaternion z component
     * @param q0 quaternion scalar component
     */
    public Quat4d(final double qx, final double qy, final double qz, final double q0)
    {
        this.x = qx;
        this.y = qy;
        this.z = qz;
        this.s = q0;
    }


    /**
     * Creates a quaternion using first 4 values of the array
     * @param q array of length >= 4, with quaternion scalar last
     */
    public Quat4d(final double[] q)
    {
        setFromArrayWithScalarLast(q);
    }


    /**
     * Creates a quaternion containing same elements as vector and scalar=0
     * @param v Vector
     */
    public Quat4d(final Vect3d v)
    {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        this.s = 1.0;
    }


    /**
     * Creates a quaternion from rotation axis and angle
     * @param axis rotation axis (must be normalized and non-zero)
     * @param angle double angle in radians
     */
    public Quat4d(final Vect3d axis, final double angle)
    {
        setFromAxisAngle(axis, angle);
    }
    
    
    /**
     * Creates a quaternion from rotation axis and angle
     * @param axis rotation axis as double[3] (must be normalized and non-zero)
     * @param angle double angle in radians
     */
    public Quat4d(final double[] axis, final double angle)
    {
        setFromAxisAngle(axis, angle);
    }
    
    
    /**
     * Sets all 4 components of this quaternion
     * @param qx quaternion x component
     * @param qy quaternion y component
     * @param qz quaternion z component
     * @param q0 quaternion scalar component
     * @return reference to this quaternion for chaining other operations
     */
    public final Quat4d set(final double qx, final double qy, final double qz, final double q0)
    {
        this.x = qx;
        this.y = qy;
        this.z = qz;
        this.s = q0;
        return this;
    }
    
    
    /**
     * Sets this quaternion to be equal to another quaternion q
     * @param q the other quaternion
     * @return reference to this quaternion for chaining other operations
     */
    public final Quat4d set(final Quat4d q)
    {
        this.x = q.x;
        this.y = q.y;
        this.z = q.z;
        this.s = q.s;
        return this;
    }
    
    
    /**
     * Sets this quaternion to identity
     * @return reference to this quaternion for chaining other operations
     */
    public final Quat4d setToIdentity()
    {
        this.x = this.y = this.z = 0.0;
        this.s = 1.0;
        return this;
    }
    
    
    /**
     * Sets all components of this quaternion to zero
     * @return reference to this quaternion for chaining other operations
     */
    public final Quat4d setToZero()
    {
        this.x = this.y = this.z = this.s = 0.0;
        return this;
    }
    
    
    /**
     * Sets the value of this quaternion from array values
     * @param q array of length >= 4 with quaternion scalar first
     * @return reference to this quaternion for chaining other operations
     */
    public final Quat4d setFromArrayWithScalarFirst(final double[] q)
    {
        this.s = q[0];
        this.x = q[1];
        this.y = q[2];
        this.z = q[3];
        return this;
    }
    
    
    /**
     * Sets the value of this quaternion from array values
     * @param q  array of length >= 4 with quaternion scalar last
     * @return reference to this quaternion for chaining other operations
     */
    public final Quat4d setFromArrayWithScalarLast(final double[] q)
    {
        this.x = q[0];
        this.y = q[1];
        this.z = q[2];
        this.s = q[3];
        return this;
    }
    
    
    /**
     * Sets the value of this quaternion from rotation axis and angle
     * @param axis rotation axis (must be normalized and non-zero)
     * @param angle double angle in radians
     * @return reference to this quaternion for chaining other operations
     */
    public final Quat4d setFromAxisAngle(final Vect3d axis, final double angle)
    {
        double sin = Math.sin(angle / 2);
        this.x = axis.x * sin;
        this.y = axis.y * sin;
        this.z = axis.z * sin;
        this.s = Math.cos(angle / 2);
        return this;
    }
    
    
    /**
     * Sets the value of this quaternion from rotation axis and angle
     * @param axis rotation axis as double[3] (must be normalized and non-zero)
     * @param angle double angle in radians
     * @return reference to this quaternion for chaining other operations
     */
    public final Quat4d setFromAxisAngle(final double[] axis, final double angle)
    {
        double sin = Math.sin(angle / 2);
        this.x = axis[0] * sin;
        this.y = axis[1] * sin;
        this.z = axis[2] * sin;
        this.s = Math.cos(angle / 2);
        return this;
    }
    
    
    /**
     * @return A fresh copy of this quaternion
     */
    public final Quat4d copy()
    {
        return new Quat4d(x, y, z, s);
    }
    
    
    /**
     * @return True if all components of this quaternion are 0
     */
    public final boolean isNull()
    {
        if (x == 0 && y == 0 && z == 0 && s == 0)
            return true;

        return false;
    }


    /**
     * Sets the value of this quaternion to its conjugate
     * @return reference to this quaternion for chaining other operations
     */
    public final Quat4d conjugate()
    {
        this.x = -this.x;
        this.y = -this.y;
        this.z = -this.z;
        return this;
    }
    
    
    /**
     * Computes the conjugate of q and places the result into this quaternion
     * @param q other quaternion
     * @return reference to this quaternion for chaining other operations
     */
    public final Quat4d conjugate(final Quat4d q)
    {
        this.x = -q.x;
        this.y = -q.y;
        this.z = -q.z;
        this.s = q.s;
        return this;
    }


    /**
     * Normalizes this quaternion in place
     * @return reference to this quaternion for chaining other operations
     */
    public final Quat4d normalize()
    {
        normalize(this);
        return this;
    }


    /**
     * Normalizes quaternion q and places the result into this quaternion
     * @param q other quaternion
     * @return reference to this quaternion for chaining other operations
     */
    public final Quat4d normalize(final Quat4d q)
    {
        double norm = (q.x * q.x + q.y * q.y + q.z * q.z + q.s * q.s);

        if (norm > 0.0)
        {
            norm = 1.0 / Math.sqrt(norm);
            this.x = norm * q.x;
            this.y = norm * q.y;
            this.z = norm * q.z;
            this.s = norm * q.s;
        }
        else
        {
            this.x = 0.0;
            this.y = 0.0;
            this.z = 0.0;
            this.s = 0.0;
        }
        
        return this;
    }
    
    
    /**
     * Computes the quaternion product of this and q, and places the result into this quaternion.
     * @param q the other quaternion
     * @return reference to this quaternion for chaining other operations
     */
    public final Quat4d mul(final Quat4d q)
    {
        double x, y, w;
        w = this.s * q.s - this.x * q.x - this.y * q.y - this.z * q.z;
        x = this.s * q.x + this.x * q.s + this.y * q.z - this.z * q.y;
        y = this.s * q.y - this.x * q.z + this.y * q.s + this.z * q.x;
        this.z = this.s * q.z + this.x * q.y - this.y * q.x + this.z * q.s;
        this.s = w;
        this.x = x;
        this.y = y;
        return this;
    }
    
    
    /**
     * Computes the quaternion product of q1 and q2, and places the result into this quaternion.<br/>
     * Note that this is not safe for aliasing (i.e. this cannot be q1 or q2).
     * @param q1 first quaternion operand
     * @param q2 second quaternion operand
     * @return reference to this quaternion for chaining other operations
     */
    public final Quat4d mulQQ(final Quat4d q1, final Quat4d q2)
    {
        this.s = q1.s * q2.s - q1.x * q2.x - q1.y * q2.y - q1.z * q2.z;
        this.x = q1.s * q2.x + q1.x * q2.s + q1.y * q2.z - q1.z * q2.y;
        this.y = q1.s * q2.y - q1.x * q2.z + q1.y * q2.s + q1.z * q2.x;
        this.z = q1.s * q2.z + q1.x * q2.y - q1.y * q2.x + q1.z * q2.s;
        return this;
    }
    
    
    /**
     * Computes the quaternion product of q1 and q2', and places the result into this quaternion.<br/>
     * Note that this is not safe for aliasing (i.e. this cannot be q1 or q2).
     * @param q1 first quaternion operand
     * @param q2 second quaternion operand
     * @return reference to this quaternion for chaining other operations
     */
    public final Quat4d mulQQtilde(final Quat4d q1, final Quat4d q2)
    {
        this.s = q1.s * q2.s + q1.x * q2.x + q1.y * q2.y + q1.z * q2.z;
        this.x = q1.x * q2.s - q1.s * q2.x - q1.y * q2.z + q1.z * q2.y;
        this.y = q1.y * q2.s - q1.s * q2.y + q1.x * q2.z - q1.z * q2.x;
        this.z = q1.z * q2.s - q1.s * q2.z - q1.x * q2.y + q1.y * q2.x;
        return this;
    }
    
    
    /**
     * Rotates vector v with this quaternion and places result in res.<br/>
     * This actually computes the double quaternion product q.v.q'.<br/>
     * Note that this is safe for aliasing (i.e. res can be v).
     * @param v the vector to rotate
     * @param res vector to store result into (must not be null)
     */
    public final void rotate(final Vect3d v, final Vect3d res)
    {
        mulQVQtilde(v, res);
    }
    
    
    /**
     * Computes double product q*v*q' where q is this quaternion and places result in res.<br/>
     * Note that this is safe for aliasing (i.e. res can be v).
     * @param v the vector to transform
     * @param res vector to store result into (must not be null)
     */
    public final void mulQVQtilde(final Vect3d v, final Vect3d res)
    {
        double twoxx = 2.0 * this.x * this.x;
        double twoyy = 2.0 * this.y * this.y;
        double twozz = 2.0 * this.z * this.z;
        
        double xy = this.x * this.y;
        double yz = this.y * this.z;
        double xz = this.x * this.z;
        double wx = this.s * this.x;
        double wy = this.s * this.y;
        double wz = this.s * this.z;
        
        double x = v.x * (1.0 - twoyy - twozz) +
                   v.y * (2.0 * (xy - wz)) +
                   v.z * (2.0 * (xz + wy));
        
        double y = v.x * (2.0 * (xy + wz)) +
                   v.y * (1.0 - twoxx - twozz) +
                   v.z * (2.0 * (yz - wx));
        
        double z = v.x * (2.0 * (xz - wy)) +
                   v.y * (2.0 * (yz + wx)) +
                   v.z * (1.0 - twoxx - twoyy);
        
        res.x = x;
        res.y = y;
        res.z = z;
    }
    
    
    /**
     * Computes double product q'*v*q where q is this quaternion and places result in res.<br/>
     * Note that this is safe for aliasing (i.e. res can be v).
     * @param v the vector to transform
     * @param res vector to store result into (must not be null)
     */
    public final void mulQtildeVQ(final Vect3d v, final Vect3d res)
    {
        double twoxx = 2.0 * this.x * this.x;
        double twoyy = 2.0 * this.y * this.y;
        double twozz = 2.0 * this.z * this.z;
        
        double xy = this.x * this.y;
        double yz = this.y * this.z;
        double xz = this.x * this.z;
        double wx = this.s * this.x;
        double wy = this.s * this.y;
        double wz = this.s * this.z;
        
        double x = v.x * (1.0 - twoyy - twozz) +
                   v.y * (2.0 * (xy + wz)) +
                   v.z * (2.0 * (xz - wy));
        
        double y = v.x * (2.0 * (xy - wz)) +
                   v.y * (1.0 - twoxx - twozz) +
                   v.z * (2.0 * (yz + wx));
        
        double z = v.x * (2.0 * (xz + wy)) +
                   v.y * (2.0 * (yz - wx)) +
                   v.z * (1.0 - twoxx - twoyy);
        
        res.x = x;
        res.y = y;
        res.z = z;
    }


    /**
     * @return New vector of Euler angles producing a rotation equivalent to this quaternion.
     * @see #toEulerAngles(Vect3d)
     */
    public final Vect3d toEulerAngles()
    {
        Vect3d euler = new Vect3d();
        toEulerAngles(euler);
        return euler;
    }


    /**
     * Computes Euler angles producing a rotation equivalent to this quaternion.
     * The order of rotation for the produced Euler Angles is z, y, x (NASA convention for heading,pitch,roll).<br/>
     * Quaternion must first be normalized
     * @param euler vector object to store computed Euler angles (must not be null)
     */
    public final void toEulerAngles(final Vect3d euler)
    {
        double sqs = s * s;
        double sqx = x * x;
        double sqy = y * y;
        double sqz = z * z;
        euler.z = Math.atan2(2.0 * (x * y + z * s), sqx - sqy - sqz + sqs); // heading
        euler.y = Math.atan2(2.0 * (y * z + x * s), -sqx - sqy + sqz + sqs); // pitch
        euler.x = Math.asin(-2.0 * (x * z - y * s)); // roll
    }
    
    
    /**
     * @return A new rotation matrix producing a rotation equivalent to this quaternion.
     * @see #toRotationMatrix(Mat3d)
     */
    public final Mat3d toRotationMatrix()
    {
        Mat3d m = new Mat3d();
        toRotationMatrix(m);
        return m;
    }
    
    
    /**
     * Computes rotation matrix producing a rotation equivalent to this quaternion.
     * @param m 3x3 matrix to store the result into (cannot be null)
     */
    public final void toRotationMatrix(final Mat3d m)
    {
        double twoxx = 2.0 * this.x * this.x;
        double twoyy = 2.0 * this.y * this.y;
        double twozz = 2.0 * this.z * this.z;
        
        double xy = this.x * this.y;
        double yz = this.y * this.z;
        double xz = this.x * this.z;
        double wx = this.s * this.x;
        double wy = this.s * this.y;
        double wz = this.s * this.z;
        
        m.m00 = (1.0 - twoyy - twozz);
        m.m10 = (2.0 * (xy + wz));
        m.m20 = (2.0 * (xz - wy));

        m.m01 = (2.0 * (xy - wz));
        m.m11 = (1.0 - twoxx - twozz);
        m.m21 = (2.0 * (yz + wx));

        m.m02 = (2.0 * (xz + wy));
        m.m12 = (2.0 * (yz - wx));
        m.m22 = (1.0 - twoxx - twoyy);
    }
    
    
    @Override
    public final boolean equals(Object o)
    {
        if (o == null || !(o instanceof Quat4d))
            return false;
        
        Quat4d q = (Quat4d)o;        
        if (q.x == this.x && q.y == this.y && q.z == this.z && q.s == this.s)
            return true;
        
        return false;
    }


    /**
     *  Performs a great circle interpolation between quaternions q1 and q2
     *  and places the result into a third quaternion.<br/>
     *  Note that this is safe for aliasing (i.e. res can be q1 or q2). 
     *  @param q1 the first quaternion
     *  @param q2 the second quaternion
     *  @param alpha the alpha interpolation parameter
     * @param res quaternion to store result into (must not be null)
     */
    public final static void interpolate(final Quat4d q1, final Quat4d q2, final double alpha, final Quat4d res)
    {
        double dot, s1, s2, om, sinom;
        dot = q2.x * q1.x + q2.y * q1.y + q2.z * q1.z + q2.s * q1.s;

        if (dot < 0)
        {
            // negate quaternion
            q1.x = -q1.x;
            q1.y = -q1.y;
            q1.z = -q1.z;
            q1.s = -q1.s;
            dot = -dot;
        }

        if ((1.0 - dot) > Mat3d.EPS)
        {
            om = Math.acos(dot);
            sinom = Math.sin(om);
            s1 = Math.sin((1.0 - alpha) * om) / sinom;
            s2 = Math.sin(alpha * om) / sinom;
        }
        else
        {
            s1 = 1.0 - alpha;
            s2 = alpha;
        }
        
        res.s = s1 * q1.s + s2 * q2.s;
        res.x = s1 * q1.x + s2 * q2.x;
        res.y = s1 * q1.y + s2 * q2.y;
        res.z = s1 * q1.z + s2 * q2.z;
    }

    
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append("[s=")
           .append(s)
           .append(", v=")
           .append(x)
           .append(',')
           .append(y)
           .append(',')
           .append(z)
           .append(']');
        return buf.toString();
    }
}
