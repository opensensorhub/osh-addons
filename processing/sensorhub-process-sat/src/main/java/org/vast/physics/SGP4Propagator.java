/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License Version
 1.1 (the "License"); you may not use this file except in compliance with
 the License. You may obtain a copy of the License at
 http://www.mozilla.org/MPL/MPL-1.1.html
 
 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.
 
 The Original Code is the "SensorML DataProcessing Engine".
 
 The Initial Developer of the Original Code is the VAST team at the
 University of Alabama in Huntsville (UAH). <http://vast.uah.edu>
 Portions created by the Initial Developer are Copyright (C) 2007
 the Initial Developer. All Rights Reserved.
 Please Contact Mike Botts <mike.botts@uah.edu> for more information.
 
 Contributor(s): 
    Alexandre Robin <robin@nsstc.uah.edu>
 
******************************* END LICENSE BLOCK ***************************/

package org.vast.physics;

import org.sensorhub.algo.geoloc.Ellipsoid;
import org.sensorhub.algo.vecmath.Vect3d;


/**
 * <p><b>Title:</b><br/> sgp4propagator</p>
 *
 * <p><b>Description:</b><br/>
 * Original code by Tony Cook and Pete Conway including conversion
 *  of sgp4propagator method from FORTRAN to java.
 *
 *  Greatly modified by Mike Botts (10/22/97) to check validity of
 *  current TLE before going off to the tleProvider again, to support
 *  more general tleProvider interface so that TLEs can come from a
 *  variety of source types (e.g. tle listings, url, gui, etc.),
 *  support for Datum assignment; also, now only outputs in ECI frame
 * </p>
 *
 * <p>Copyright (c) 2005</p>
 * @author Pete Conway, Tony Cook, Mike Botts
 * @since 2/17/98
 */
public class SGP4Propagator
{
	//private Datum datum;
	private double ae, re;


	public SGP4Propagator()
	{
		// set default Datum
		setDatum(Ellipsoid.WGS84);
	}


	public void setDatum(Ellipsoid datum)
	{
		//this.datum = datum;
		ae = datum.getPolarRadius() / datum.getEquatorRadius();
		re = datum.getEquatorRadius() / 1000; // scale to km for sgp4
	}


	/* *********** primary work code *************************/
	//  getsgp4() - computes geolocation based on tle
	//  and requested time; sets and returns eci
	//  Converted from Norad sgp4Propagator (FORTRAN version)
	public MechanicalState getECIOrbitalState(double time, TLEInfo tle)
	{
		// Local variables
		double beta, coef, eeta, delm, delo, aodp, capu, xmdf, aynl, elsq, temp, eosq, rdot, cosu, sinu, coef1, t2cof, t3cof, t4cof, t5cof, temp1, temp2, temp3, temp4, temp5, temp6, cos2u, sin2u, a, e, s, r, u, betal, betao, omega, ecose, delmo, aycof, tcube, esine, a3ovk2, tempa, cosik, tempe, cosio, etasq, xmcof, a1, sinio, x3thm1, qoms24, c2, c1, xnodp, c3, x1mth2, c4, c5, xmdot, psisq, x1m5th, xlcof, sinmo, x7thm1, d2, d3, d4, s4, xnode, templ, tfour, rfdot, betao2, xinck, rdotk, sinuk, cosuk, sinik, theta2, theta4, xhdot1, ao, qoms2t, /*dragcf,*/pl, omgadf, rk, qo, uk, so, xl, xn, delomg, omgcof, perige, ux, uy, uz, vx, xnodcf, xnoddf, vy, vz, /*xmnpda,*/xnodek, omgdot, rfdotk, e6a, cosnok, ck2, cosepw, ck4, /*rdprmn,*/tothrd, sinepw, sinnok, xnodot, pinvsq, xj2, xj3, xj4, eta, axn;
		double xke, ayn, xxxx, tsi, xll, xmp, tsq, xlt, xmx, xmy, del1, c1sq, d__1, d__2;
		int i, isimp;

		// establish main input
		double tsince = (time - tle.getTleTime()) / 60.0;
		double xmo = tle.getMeanAnomaly();
		double xnodeo = tle.getRightAscension();
		double omegao = tle.getArgumentOfPerigee();
		double eo = tle.getEccentricity();
		double xincl = tle.getInclination();
		double xno = tle.getMeanMotion() * 60.0; // need rad/min ??
		double bstar = tle.getBStar();

		//      BROUWER THEORY
		//     CONSTANTS
		xj2 = 1.08263e-3;
		xj3 = -2.53881e-6;
		xj4 = -1.65597e-6;

		//  Inialization of values
		d2 = 0.0;
		d3 = 0.0;
		d4 = 0.0;
		t3cof = 0.0;
		t4cof = 0.0;
		t5cof = 0.0;
		temp4 = 0.0;
		temp5 = 0.0;
		temp6 = 0.0;
		cosepw = 0.0;
		sinepw = 0.0;

		// Computing 2nd power
		d__1 = ae;
		ck2 = xj2 * .5 * (d__1 * d__1);

		// Computing 4th power
		d__1 = ae;
		d__1 *= d__1;
		ck4 = xj4 * -.375 * (d__1 * d__1);
		e6a = 1e-6;
		qo = 120.;
		so = 78.;

		// Computing 4th power
		d__1 = (qo - so) * ae / re;
		d__1 *= d__1;
		qoms2t = d__1 * d__1;
		s = ae * (so / re + 1.);
		tothrd = 2.0 / 3.0; // MEB 10/21/98
		//tothrd = .66666666666666663;
		xke = .0743669161;

		//  WGS72 EQ MU IS  398600.8D0   !!!!
		//xmnpda = 1440.;  // NEVER USED

		//     RESET EO TO NONZERO VALUE
		if (eo == 0.)
		{
			eo = 1e-10;
		}
		
		//     RECOVER ORIGINAL MEAN MOTION (XNODP) AND SEMIMAJOR AXIS (AODP)
		//     FROM INPUT ELEMENTS
		d__1 = xke / xno;
		a1 = Math.pow(d__1, tothrd);
		cosio = Math.cos(xincl);
		theta2 = cosio * cosio;
		x3thm1 = theta2 * 3. - 1.;
		eosq = eo * eo;
		betao2 = 1. - eosq;
		betao = Math.sqrt(betao2);
		del1 = ck2 * 1.5 * x3thm1 / (a1 * a1 * betao * betao2);
		ao = a1 * (1. - del1 * (tothrd * .5 + del1 * (del1 * 1.654320987654321 + 1.)));
		delo = ck2 * 1.5 * x3thm1 / (ao * ao * betao * betao2);
		xnodp = xno / (delo + 1.);
		aodp = ao / (1. - delo);

		//     INITIALIZATION
		//     FOR PERIGEE LESS THAN 220 KILOMETERS, THE ISIMP FLAG IS SET AND
		//     THE EQUATIONS ARE TRUNCATED TO LINEAR VARIATION IN SQRT A AND
		//     QUADRATIC VARIATION IN MEAN ANOMALY.  ALSO, THE C3 TERM, THE
		//     DELTA OMEGA TERM, AND THE DELTA M TERM ARE DROPPED.
		isimp = 0;

		if (aodp * (1. - eo) / ae < 220. / re + ae)
		{
			isimp = 1;
		}
		
		//     FOR PERIGEE BELOW 156 KM, THE VALUES OF
		//     S AND QOMS2T ARE ALTERED
		s4 = s;
		qoms24 = qoms2t;
		perige = (aodp * (1. - eo) - ae) * re;
		if (perige <= 156.)
		{
			s4 = perige - 78.;
			if (perige > 98.)
			{
				// Computing 4th power
				d__1 = (120. - s4) * ae / re;
				d__1 *= d__1;
				qoms24 = d__1 * d__1;
				s4 = s4 / re + ae;
			}
			else
				s4 = 20.;
		}
		pinvsq = 1. / (aodp * aodp * betao2 * betao2);
		tsi = 1. / (aodp - s4);
		eta = aodp * eo * tsi;
		etasq = eta * eta;
		eeta = eo * eta;
		d__1 = 1. - etasq;
		psisq = Math.abs(d__1);

		// Computing 4th power
		d__1 = tsi;
		d__1 *= d__1;
		coef = qoms24 * (d__1 * d__1);
		coef1 = coef / Math.pow(psisq, 3.5);
		c2 = coef1 * xnodp * (aodp * (etasq * 1.5 + 1. + eeta * (etasq + 4.)) + ck2 * .75 * tsi / psisq * x3thm1 * (etasq * 3. * (etasq + 8.) + 8.));
		c1 = bstar * c2;
		sinio = Math.sin(xincl);

		// Computing 3rd power
		d__1 = ae;
		d__2 = d__1;
		a3ovk2 = -xj3 / ck2 * (d__2 * (d__1 * d__1));
		c3 = coef * tsi * a3ovk2 * xnodp * ae * sinio / eo;
		x1mth2 = 1. - theta2;
		c4 = xnodp
				* 2.
				* coef1
				* aodp
				* betao2
				* (eta * (etasq * .5 + 2.) + eo * (etasq * 2. + .5) - ck2 * 2. * tsi / (aodp * psisq)
						* (x3thm1 * -3. * (1. - eeta * 2. + etasq * (1.5 - eeta * .5)) + x1mth2 * .75 * (etasq * 2. - eeta * (etasq + 1.)) * Math.cos(omegao * 2.)));
		c5 = coef1 * 2. * aodp * betao2 * ((etasq + eeta) * 2.75 + 1. + eeta * etasq);
		theta4 = theta2 * theta2;
		temp1 = ck2 * 3. * pinvsq * xnodp;
		temp2 = temp1 * ck2 * pinvsq;
		temp3 = ck4 * 1.25 * pinvsq * pinvsq * xnodp;
		xmdot = xnodp + temp1 * .5 * betao * x3thm1 + temp2 * .0625 * betao * (13. - theta2 * 78. + theta4 * 137.);
		x1m5th = 1. - theta2 * 5.;
		omgdot = temp1 * -.5 * x1m5th + temp2 * .0625 * (7. - theta2 * 114. + theta4 * 395.) + temp3 * (3. - theta2 * 36. + theta4 * 49.);
		xhdot1 = -temp1 * cosio;
		xnodot = xhdot1 + (temp2 * .5 * (4. - theta2 * 19.) + temp3 * 2. * (3. - theta2 * 7.)) * cosio;
		omgcof = bstar * c3 * Math.cos(omegao);
		xmcof = -tothrd * coef * bstar * ae / eeta;
		xnodcf = betao2 * 3.5 * xhdot1 * c1;
		t2cof = c1 * 1.5;
		xlcof = a3ovk2 * .125 * sinio * (cosio * 5. + 3.) / (cosio + 1.);
		aycof = a3ovk2 * .25 * sinio;

		// Computing 3rd power
		d__1 = eta * Math.cos(xmo) + 1.;
		d__2 = d__1;
		delmo = d__2 * (d__1 * d__1);
		sinmo = Math.sin(xmo);
		x7thm1 = theta2 * 7. - 1.;
		if (isimp != 1)
		{
			c1sq = c1 * c1;
			d2 = aodp * 4. * tsi * c1sq;
			temp = d2 * tsi * c1 / 3.;
			d3 = (aodp * 17. + s4) * temp;
			d4 = temp * .5 * aodp * tsi * (aodp * 221. + s4 * 31.) * c1;
			t3cof = d2 + c1sq * 2.;
			t4cof = (d3 * 3. + c1 * (d2 * 12. + c1sq * 10.)) * .25;
			t5cof = (d4 * 3. + c1 * 12. * d3 + d2 * 6. * d2 + c1sq * 15. * (d2 * 2. + c1sq)) * .2;
		}

		//     UPDATE FOR SECULAR GRAVITY AND ATMOSPHERIC DRAG
		xmdf = xmo + xmdot * tsince;
		omgadf = omegao + omgdot * tsince;
		xnoddf = xnodeo + xnodot * tsince;
		omega = omgadf;
		xmp = xmdf;
		tsq = tsince * tsince;
		xnode = xnoddf + xnodcf * tsq;
		tempa = 1. - c1 * tsince;
		tempe = bstar * c4 * tsince;

		templ = t2cof * tsq;

		if (isimp != 1)
		{
			delomg = omgcof * tsince;

			// Computing 3rd power
			d__1 = eta * Math.cos(xmdf) + 1.;
			d__2 = d__1;
			delm = xmcof * (d__2 * (d__1 * d__1) - delmo);
			temp = delomg + delm;
			xmp = xmdf + temp;
			omega = omgadf - temp;
			tcube = tsq * tsince;
			tfour = tsince * tcube;
			tempa = tempa - d2 * tsq - d3 * tcube - d4 * tfour;
			tempe += bstar * c5 * (Math.sin(xmp) - sinmo);
			templ = templ + t3cof * tcube + tfour * (t4cof + tsince * t5cof);
		}

		// Computing 2nd power
		d__1 = tempa;
		a = aodp * (d__1 * d__1);
		e = eo - tempe;
		// rdprmn = xnodp * (float)1.5 * c1; // NEVER USED

		//  RAD/MIN^2
		//dragcf = xnodp * (float)1.5 * c2;  //NEVER USED
		xl = xmp + omega + xnode + xnodp * templ;
		beta = Math.sqrt(1. - e * e);
		xn = xke / Math.pow(a, 1.5);

		//     LONG PERIOD PERIODICS
		axn = e * Math.cos(omega);
		temp = 1. / (a * beta * beta);
		xll = temp * xlcof * axn;
		aynl = temp * aycof;
		xlt = xl + xll;
		ayn = e * Math.sin(omega) + aynl;

		//     SOLVE KEPLERS EQUATION
		d__1 = xlt - xnode;

		capu = getfmod2p(d__1);
		temp2 = capu;

		xxxx = 0.0;
		for (i = 1; i <= 10; ++i)
		{
			sinepw = Math.sin(temp2);
			cosepw = Math.cos(temp2);
			temp3 = axn * sinepw;
			temp4 = ayn * cosepw;
			temp5 = axn * cosepw;
			temp6 = ayn * sinepw;
			xxxx = (capu - temp4 + temp3 - temp2) / (1. - temp5 - temp6) + temp2;

			d__1 = xxxx - temp2;

			if (Math.abs(d__1) >= e6a)
			{
				temp2 = xxxx;
			}
		}

		//     SHORT PERIOD PRELIMINARY QUANTITIES
		ecose = temp5 + temp6;
		esine = temp3 - temp4;

		elsq = axn * axn + ayn * ayn;
		temp = 1. - elsq;
		pl = a * temp;
		r = a * (1. - ecose);
		temp1 = 1. / r;
		rdot = xke * Math.sqrt(a) * esine * temp1;
		rfdot = xke * Math.sqrt(pl) * temp1;
		temp2 = a * temp1;
		betal = Math.sqrt(temp);
		temp3 = 1. / (betal + 1.);
		cosu = temp2 * (cosepw - axn + ayn * esine * temp3);
		sinu = temp2 * (sinepw - ayn - axn * esine * temp3);

		u = Math.atan2(sinu, cosu);
		sin2u = sinu * 2. * cosu;
		cos2u = cosu * 2. * cosu - 1.;
		temp = 1. / pl;
		temp1 = ck2 * temp;
		temp2 = temp1 * temp;

		//     UPDATE FOR SHORT PERIODICS
		rk = r * (1. - temp2 * 1.5 * betal * x3thm1) + temp1 * .5 * x1mth2 * cos2u;
		uk = u - temp2 * .25 * x7thm1 * sin2u;
		xnodek = xnode + temp2 * 1.5 * cosio * sin2u;
		xinck = xincl + temp2 * 1.5 * cosio * sinio * cos2u;
		rdotk = rdot - xn * temp1 * x1mth2 * sin2u;
		rfdotk = rfdot + xn * temp1 * (x1mth2 * cos2u + x3thm1 * 1.5);

		//     ORIENTATION VECTORS
		sinuk = Math.sin(uk);
		cosuk = Math.cos(uk);
		sinik = Math.sin(xinck);
		cosik = Math.cos(xinck);
		sinnok = Math.sin(xnodek);
		cosnok = Math.cos(xnodek);
		xmx = -sinnok * cosik;
		xmy = cosnok * cosik;
		ux = xmx * sinuk + cosnok * cosuk;
		uy = xmy * sinuk + sinnok * cosuk;
		uz = sinik * sinuk;
		vx = xmx * cosuk - cosnok * sinuk;
		vy = xmy * cosuk - sinnok * sinuk;
		vz = sinik * cosuk;

		Vect3d eciPosition = new Vect3d(rk * ux, rk * uy, rk * uz);
		Vect3d eciVelocity = new Vect3d(rdotk * ux + rfdotk * vx, rdotk * uy + rfdotk * vy, rdotk * uz + rfdotk * vz);

		double pole = re / ae * 1000; // scale back to meters
		double timeFactor = pole * 1440.0 / 86400.;
		eciPosition.scale(pole);
		eciVelocity.scale(timeFactor);

		MechanicalState state = new MechanicalState();
		state.julianTime = time;
		state.linearPosition = eciPosition;
		state.linearVelocity = eciVelocity;

		return state;
	}


	private double getfmod2p(double x)
	{
		// System generated locals 
		double ret_val = 0.0;

		// CONSTANTS 
		double pi = Math.PI;
		double tpi = pi * 2;

		ret_val = x;
		int i = (int) (ret_val / tpi);
		ret_val -= i * tpi;
		if (ret_val < 0.)
			ret_val += tpi;

		return ret_val;
	}
}
