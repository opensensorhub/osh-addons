/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.algo.vecmath;

import static org.junit.Assert.*;
import org.junit.Test;
import org.sensorhub.algo.vecmath.Quat4d;
import org.sensorhub.algo.vecmath.Vect3d;


public class TestVect3d
{
    static final double EPS = 1.0e-15;
    
    
    @Test
    public void testInitFromScalars()
    {
        double x = 1.0, y = 2.0, z = 3.0;
        Vect3d v = new Vect3d(x, y, z);        
        assertEquals(x, v.x, 0.0);
        assertEquals(y, v.y, 0.0);
        assertEquals(z, v.z, 0.0);
    }
    
    
    @Test
    public void testInitFromArray()
    {
        double[] a = new double[] {1.0, 2.0, 3.0};
        Vect3d v = new Vect3d(a);        
        assertEquals(a[0], v.x, 0.0);
        assertEquals(a[1], v.y, 0.0);
        assertEquals(a[2], v.z, 0.0);
    }
    
    
    @Test
    public void testSetFromArray()
    {
        Vect3d v = new Vect3d();
        
        double[] a = new double[] {1.0, 2.0, 3.0};
        v.setFromArray(a);
        assertEquals(a[0], v.x, 0.0);
        assertEquals(a[1], v.y, 0.0);
        assertEquals(a[2], v.z, 0.0);
        
        double[] b = new double[] {4.0, 5.0, 6.0};
        v.setFromArray(b);
        assertEquals(b[0], v.x, 0.0);
        assertEquals(b[1], v.y, 0.0);
        assertEquals(b[2], v.z, 0.0);
    }
    
    
    @Test
    public void testSetToZero()
    {
        Vect3d v = new Vect3d(1.0, 2.0, 3.0);
        v.setToZero();
        assertEquals(0.0, v.x, 0.0);
        assertEquals(0.0, v.y, 0.0);
        assertEquals(0.0, v.z, 0.0);
    }
    

    @Test
    public void testNorm()
    {
        Vect3d v;
        
        v = new Vect3d(1.0, 1.0, 1.0);
        assertEquals(3.0, v.normSq(), 1e-20);
        assertEquals(Math.sqrt(3.0), v.norm(), EPS);
        
        v = new Vect3d(1.0, 2.0, 3.0);
        assertEquals(14.0, v.normSq(), 1e-20);
        assertEquals(Math.sqrt(14.0), v.norm(), EPS);
    }
    
    
    @Test
    public void testNormalize()
    {
        Vect3d v;
        double oldNorm;
        
        v = new Vect3d(100.0, 100.0, 100.0);
        v.normalize();
        assertEquals(1.0, v.norm(), EPS);
        assertEquals(1.0/Math.sqrt(3), v.x, 0.0);
        assertEquals(1.0/Math.sqrt(3), v.y, 0.0);
        assertEquals(1.0/Math.sqrt(3), v.z, 0.0);
        
        v = new Vect3d(100.0, 200.0, 300.0);
        oldNorm = v.norm();
        v.normalize();
        assertEquals(1.0, v.norm(), EPS);
        assertEquals(100.0/oldNorm, v.x, EPS);
        assertEquals(200.0/oldNorm, v.y, EPS);
        assertEquals(300.0/oldNorm, v.z, EPS);
        
        v = new Vect3d(12.48989, -978.236486, 326.798);
        Vect3d res = new Vect3d().normalize(v);
        assertEquals(1.0, res.norm(), EPS);
        assertEquals(v.x/v.norm(), res.x, EPS);
        assertEquals(v.y/v.norm(), res.y, EPS);
        assertEquals(v.z/v.norm(), res.z, EPS);
    }
    
    
    @Test
    public void testScale()
    {
        Vect3d v;
        
        v = new Vect3d(1.0, 2.0, 3.0);
        v.scale(0.5);
        assertEquals(0.5, v.x, EPS);
        assertEquals(1.0, v.y, EPS);
        assertEquals(1.5, v.z, EPS);
    }
    
    
    @Test
    public void testAdd()
    {
        Vect3d v1 = new Vect3d(5.0,  -10.0, 78.0);
        Vect3d v2 = new Vect3d(-56.0, 23.0,  8.0);
        
        v1.add(v2);
        assertEquals(-51.0, v1.x, EPS);
        assertEquals( 13.0, v1.y, EPS);
        assertEquals( 86.0, v1.z, EPS);
        
        Vect3d res = new Vect3d().add(v1, v2);
        assertEquals(-51.0, v1.x, EPS);
        assertEquals( 13.0, v1.y, EPS);
        assertEquals( 86.0, v1.z, EPS);        
        assertEquals(-107.0, res.x, EPS);
        assertEquals(  36.0, res.y, EPS);
        assertEquals(  94.0, res.z, EPS);
    }
    
    
    @Test
    public void testSub()
    {
        Vect3d v1 = new Vect3d(5.0,  -10.0, 78.0);
        Vect3d v2 = new Vect3d(-56.0, 23.0,  8.0);
        
        v1.sub(v2);
        assertEquals( 61.0, v1.x, EPS);
        assertEquals(-33.0, v1.y, EPS);
        assertEquals( 70.0, v1.z, EPS);
        
        Vect3d res = new Vect3d().sub(v1, v2);
        assertEquals( 61.0, v1.x, EPS);
        assertEquals(-33.0, v1.y, EPS);
        assertEquals( 70.0, v1.z, EPS);        
        assertEquals(117.0, res.x, EPS);
        assertEquals(-56.0, res.y, EPS);
        assertEquals( 62.0, res.z, EPS);
    }
    
    
    @Test
    public void testDot()
    {
        Vect3d v1 = new Vect3d(1.0, 2.0, 3.0);
        Vect3d v2 = new Vect3d(4.0, 5.0, 6.0);
        
        double dot = v1.dot(v2);
        assertEquals(32.0, dot, EPS);
    }
    
    
    @Test
    public void testCross()
    {
        Vect3d v1 = new Vect3d(1.0, 2.0, 3.0);
        Vect3d v2 = new Vect3d(4.0, 5.0, 6.0);
        
        v1.cross(v2);
        assertEquals(2.0*6.0-5.0*3.0, v1.x, EPS);
        assertEquals(3.0*4.0-1.0*6.0, v1.y, EPS);
        assertEquals(1.0*5.0-4.0*2.0, v1.z, EPS);
        
        v1.set(1.0, 2.0, 3.0);
        v2.set(4.0, 5.0, 6.0);
        
        Vect3d res = new Vect3d().cross(v1, v2);
        assertEquals(1.0, v1.x, EPS);
        assertEquals(2.0, v1.y, EPS);
        assertEquals(3.0, v1.z, EPS);
        assertEquals(2.0*6.0-5.0*3.0, res.x, EPS);
        assertEquals(3.0*4.0-1.0*6.0, res.y, EPS);
        assertEquals(1.0*5.0-4.0*2.0, res.z, EPS);
    }
    
    
    @Test
    public void testSeparationAngle()
    {
        Vect3d v1 = new Vect3d();
        Vect3d v2 = new Vect3d();
        
        v1.set(1.0, 0.0, 0.0);
        v2.set(1.0, 0.0, 0.0);
        assertEquals(0.0, v1.separationAngle(v1), EPS);
        assertEquals(0.0, v1.separationAngle(v2), EPS);
        
        v1.set(1.0, 0.0, 0.0);
        v2.set(0.0, 1.0, 0.0);
        assertEquals(Math.PI/2., v1.separationAngle(v2), EPS);
        
        v1.set(1.0, 0.0, 0.0);
        v2.set(-1.0, 0.0, 0.0);
        assertEquals(Math.PI, v1.separationAngle(v2), EPS);
        
        v1.set(1.0, 0.0, 0.0);
        v2.set(1.0, 1.0, 0.0);
        assertEquals(Math.PI/4, v1.separationAngle(v2), EPS);
        
        v1.set(1.0, 0.0, 0.0);
        v2.set(1.0, -1.0, 0.0);
        assertEquals(Math.PI/4, v1.separationAngle(v2), EPS);
    }
    
    
    @Test
    public void testRotateWithQuat()
    {
        Vect3d v = new Vect3d();
        Vect3d axis = new Vect3d();
        Quat4d q = new Quat4d();
        
        // rotate 90° around x
        v.set(1.0, 2.0, 3.0);
        axis.set(1.0, 0.0, 0.0);
        q.setFromAxisAngle(axis, Math.PI/2.0);
        v.rotate(q);        
        assertEquals(1.0, v.x, EPS);
        assertEquals(-3.0, v.y, EPS);
        assertEquals(2.0, v.z, EPS);
        
        // rotate 90° around Y
        v.set(1.0, 2.0, 3.0);
        axis.set(0.0, 1.0, 0.0);
        q.setFromAxisAngle(axis, Math.PI/2.0);
        v.rotate(q);        
        assertEquals(3.0, v.x, EPS);
        assertEquals(2.0, v.y, EPS);
        assertEquals(-1.0, v.z, EPS);
        
        // rotate 90° around Z
        v.set(1.0, 2.0, 3.0);
        axis.set(0.0, 0.0, 1.0);
        q.setFromAxisAngle(axis, Math.PI/2.0);
        v.rotate(q);        
        assertEquals(-2.0, v.x, EPS);
        assertEquals(1.0, v.y, EPS);
        assertEquals(3.0, v.z, EPS);
    }
    
    
    @Test
    public void testRotateByAxis()
    {
        Vect3d v = new Vect3d(1.0, 2.0, 3.0);
        
        // rotate 90° around x
        v.rotateX(Math.PI/2);        
        assertEquals(1.0, v.x, EPS);
        assertEquals(-3.0, v.y, EPS);
        assertEquals(2.0, v.z, EPS);
        
        // rotate 90° around Y
        v.rotateY(Math.PI/2); 
        assertEquals(2.0, v.x, EPS);
        assertEquals(-3.0, v.y, EPS);
        assertEquals(-1.0, v.z, EPS);
        
        // rotate 90° around Z
        v.rotateZ(Math.PI/2); 
        assertEquals(3.0, v.x, EPS);
        assertEquals(2.0, v.y, EPS);
        assertEquals(-1.0, v.z, EPS);
    }
    
}
