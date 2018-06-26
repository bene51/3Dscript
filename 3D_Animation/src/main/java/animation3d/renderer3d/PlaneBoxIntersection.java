package animation3d.renderer3d;

import java.util.Arrays;
import java.util.Comparator;

/**
 * ref:
 * http://www.asawicki.info/news_1428_finding_polygon_of_plane-aabb_intersection.html
 */
public class PlaneBoxIntersection {

	public static int calculateIntersection(float[] ray, float d, float[] bb0, float[] bb1, Point[] poly) {
		float[] plane = new float[] {ray[0], ray[1], ray[2], d};
		int n = calcPlaneAABBIntersectionPoints(plane, bb0, bb1, poly);
		sortPoints(poly, n, plane);
		return n;
	}

	// OutVD > 0 means ray is back-facing the plane
	// returns false if there is no intersection because ray is perpedicular to plane
	private static boolean rayToPlane(float[] rayOrig, float[] rayDir, float[] plane, float[] OutT_OutVD) {
		float OutVD = plane[0] * rayDir[0] + plane[1] * rayDir[1] + plane[2] * rayDir[2];
		if(OutVD == 0f)
			return false;

		float OutT = -(plane[0] * rayOrig[0] + plane[1] * rayOrig[1] + plane[2] * rayOrig[2] + plane[3]) / OutVD;

		OutT_OutVD[0] = OutT;
		OutT_OutVD[1] = OutVD;
		return true;
	}

	/**
	 * returns a + b * c
	 */
	private static float[] addMul(float[] a, float[] b, float c) {
		return new float[] {
				a[0] + b[0] * c,
				a[1] + b[1] * c,
				a[2] + b[2] * c
		};
	}

	// Maximum out_point_count == 6, so out_points must point to 6-element array.
	// out_point_count == 0 mean no intersection.
	// out_points are not sorted.
	private static int calcPlaneAABBIntersectionPoints(float[] plane,
			float[] aabb_min, float[] aabb_max,
			Point[] out_points) {

		int out_point_count = 0;
		float[] t_vd = new float[2];

		float[] dir = new float[3];
		float[] orig = new float[3];

		// Test edges along X axis, pointing right.
		dir[0] = aabb_max[0] - aabb_min[0]; dir[1] = 0; dir[2] = 0;
		orig[0] = aabb_min[0]; orig[1] = aabb_min[1]; orig[2] = aabb_min[2];
		if(rayToPlane(orig, dir, plane, t_vd) && t_vd[0] >= 0f && t_vd[0] < 1f)
			out_points[out_point_count++] = new Point(addMul(orig, dir, t_vd[0]));

		orig[0] = aabb_min[0]; orig[1] = aabb_max[1]; orig[2] = aabb_min[2];
		if(rayToPlane(orig, dir, plane, t_vd) && t_vd[0] >= 0f && t_vd[0] < 1f)
			out_points[out_point_count++] = new Point(addMul(orig, dir, t_vd[0]));

		orig[0] = aabb_min[0]; orig[1] = aabb_min[1]; orig[2] = aabb_max[2];
		if(rayToPlane(orig, dir, plane, t_vd) && t_vd[0] >= 0f && t_vd[0] < 1f)
			out_points[out_point_count++] = new Point(addMul(orig, dir, t_vd[0]));

		orig[0] = aabb_min[0]; orig[1] = aabb_max[1]; orig[2] = aabb_max[2];
		if(rayToPlane(orig, dir, plane, t_vd) && t_vd[0] >= 0f && t_vd[0] < 1f)
			out_points[out_point_count++] = new Point(addMul(orig, dir, t_vd[0]));


		// Test edges along Y axis, pointing up.
		dir[0] = 0; dir[1] = aabb_max[1] - aabb_min[1]; dir[2] = 0;
		orig[0] = aabb_min[0]; orig[1] = aabb_min[1]; orig[2] = aabb_min[2];
		if(rayToPlane(orig, dir, plane, t_vd) && t_vd[0] >= 0f && t_vd[0] < 1f)
			out_points[out_point_count++] = new Point(addMul(orig, dir, t_vd[0]));

		orig[0] = aabb_max[0]; orig[1] = aabb_min[1]; orig[2] = aabb_min[2];
		if(rayToPlane(orig, dir, plane, t_vd) && t_vd[0] >= 0f && t_vd[0] < 1f)
			out_points[out_point_count++] = new Point(addMul(orig, dir, t_vd[0]));

		orig[0] = aabb_min[0]; orig[1] = aabb_min[1]; orig[2] = aabb_max[2];
		if(rayToPlane(orig, dir, plane, t_vd) && t_vd[0] >= 0f && t_vd[0] < 1f)
			out_points[out_point_count++] = new Point(addMul(orig, dir, t_vd[0]));

		orig[0] = aabb_max[0]; orig[1] = aabb_min[1]; orig[2] = aabb_max[2];
		if(rayToPlane(orig, dir, plane, t_vd) && t_vd[0] >= 0f && t_vd[0] < 1f)
			out_points[out_point_count++] = new Point(addMul(orig, dir, t_vd[0]));


		// Test edges along Z axis, pointing forward.
		dir[0] = 0; dir[1] = 0; dir[2] = aabb_max[2] - aabb_min[2];
		orig[0] = aabb_min[0]; orig[1] = aabb_min[1]; orig[2] = aabb_min[2];
		if(rayToPlane(orig, dir, plane, t_vd) && t_vd[0] >= 0f && t_vd[0] < 1f)
			out_points[out_point_count++] = new Point(addMul(orig, dir, t_vd[0]));

		orig[0] = aabb_max[0]; orig[1] = aabb_min[1]; orig[2] = aabb_min[2];
		if(rayToPlane(orig, dir, plane, t_vd) && t_vd[0] >= 0f && t_vd[0] < 1f)
			out_points[out_point_count++] = new Point(addMul(orig, dir, t_vd[0]));

		orig[0] = aabb_min[0]; orig[1] = aabb_max[1]; orig[2] = aabb_min[2];
		if(rayToPlane(orig, dir, plane, t_vd) && t_vd[0] >= 0f && t_vd[0] < 1f)
			out_points[out_point_count++] = new Point(addMul(orig, dir, t_vd[0]));

		orig[0] = aabb_max[0]; orig[1] = aabb_max[1]; orig[2] = aabb_min[2];
		if(rayToPlane(orig, dir, plane, t_vd) && t_vd[0] >= 0f && t_vd[0] < 1f)
			out_points[out_point_count++] = new Point(addMul(orig, dir, t_vd[0]));

		for(int i = out_point_count; i < out_points.length; i++)
			out_points[i] = null;

		return out_point_count;
	}

	// TODO optimize sorting
	private static void sortPoints(Point[] points, int nPoints, float[] plane) {
		if(points.length == 0)
			return;

		final float[] plane_normal = new float[] {plane[0], plane[1], plane[2]};
		final Point origin = points[0];

		Arrays.sort(points, 0, nPoints, new Comparator<Point>() {
			@Override
			public int compare(Point lhs, Point rhs) {
				float lhsx = lhs.c[0] - origin.c[0];
				float lhsy = lhs.c[1] - origin.c[1];
				float lhsz = lhs.c[2] - origin.c[2];

				float rhsx = rhs.c[0] - origin.c[0];
				float rhsy = rhs.c[1] - origin.c[1];
				float rhsz = rhs.c[2] - origin.c[2];

				float cx = lhsy * rhsz - lhsz * rhsy;
				float cy = lhsz * rhsx - lhsx * rhsz;
				float cz = lhsx * rhsy - lhsy * rhsx;

				// dot = dot(plane_normal, cross(lhs - origin, rhs - origin)
				float dot = plane_normal[0] * cx + plane_normal[1] * cy + plane_normal[2] * cz;

				if(dot < 0) return -1;
				if(dot > 0) return 1;
				return 0;
			}
		});
	}

	public static class Point {
		public final float[] c;

		Point(float[] coords) {
			this.c = coords;
		}
	}

	/*
	void sort_points(D3DXVECTOR3 *points, unsigned point_count, const D3DXPLANE &plane)
	{
	    if (point_count == 0) return;

	    const D3DXVECTOR3 plane_normal = D3DXVECTOR3(plane.a, plane.b, plane.c);
	    const D3DXVECTOR3 origin = points[0];

	    std::sort(points, points + point_count, [&](const D3DXVECTOR3 &lhs, const D3DXVECTOR3 &rhs) -> bool {
	        D3DXVECTOR3 v;
	        D3DXVec3Cross(&v, &(lhs - origin), &(rhs - origin));
	        return D3DXVec3Dot(&v, &plane_normal) < 0;
	    } );
	}
	*/

}
