package povray;

public class PovrayTemplate {

	public static final String text =
			"//--------------------------------------------------------------------------\n" +
			"#version 3.7;\n" +
			"global_settings{ assumed_gamma 1.0 max_trace_level 256}\n" +
			"#default{ finish{ ambient 0.1 diffuse 0.9 }}\n" +
			"//--------------------------------------------------------------------------\n" +
			"#include \"colors.inc\"\n" +
			"#include \"textures.inc\"\n" +
			"#include \"glass.inc\"\n" +
			"#include \"metals.inc\"\n" +
			"#include \"golds.inc\"\n" +
			"#include \"stones.inc\"\n" +
			"#include \"woods.inc\"\n" +
			"#include \"shapes.inc\"\n" +
			"#include \"shapes2.inc\"\n" +
			"#include \"functions.inc\"\n" +
			"#include \"math.inc\"\n" +
			"#include \"transforms.inc\"\n" +
			"//--------------------------------------------------------------------------\n" +
			"// camera ------------------------------------------------------------------\n" +
			"#declare Camera_0 = camera {perspective angle 60               // front view\n" +
			"                            location  <0.0 , 2.0 ,-3.0>\n" +
			"                            right     x*image_width/image_height\n" +
			"                            look_at   <0.0 , 1.5 , 0.0>}\n" +
			"#declare Camera_1 = camera {/*ultra_wide_angle*/ angle 90   // diagonal view\n" +
			"                            location  <2.0 , 2.5 ,-3.0>\n" +
			"                            right     x*image_width/image_height\n" +
			"                            look_at   <0.0 , 1.0 , 0.0>}\n" +
			"#declare Camera_2 = camera {/*ultra_wide_angle*/ angle 90  //right side view\n" +
			"                            location  <3.0 , 1.0 , 0.0>\n" +
			"                            right     x*image_width/image_height\n" +
			"                            look_at   <0.0 , 1.0 , 0.0>}\n" +
			"#declare Camera_3 = camera {/*ultra_wide_angle*/ angle 90        // top view\n" +
			"                            location  <0.0 , 3.0 ,-0.001>\n" +
			"                            right     x*image_width/image_height\n" +
			"                            look_at   <0.0 , 1.0 , 0.0>}\n" +
			"camera{Camera_0}\n" +
			"// sun ----------------------------------------------------------------------\n" +
			"light_source{< 3000,3000,-3000> color White}\n" +
			"// sky ----------------------------------------------------------------------\n" +
			"sky_sphere { pigment { gradient <0,1,0>\n" +
			"                       color_map { [0.00 rgb <0.6,0.7,1.0>]\n" +
			"                                   [0.35 rgb <0.1,0.0,0.8>]\n" +
			"                                   [0.65 rgb <0.1,0.0,0.8>]\n" +
			"                                   [1.00 rgb <0.6,0.7,1.0>]\n" +
			"                       }\n" +
			"                       scale 2\n" +
			"             }\n" +
			"}\n" +
			"\n" +
			"//---------------------------------------------------------------------------\n" +
			"//---------------------------- objects in scene -----------------------------\n" +
			"//---------------------------------------------------------------------------\n" +
			"\n" +
			"// floor\n" +
			"plane{<0,1,0>,0\n" +
			"    texture{ T_Stone30  scale 3 }\n" +
			"    finish { reflection 0 }\n" +
			"}\n" +
			"\n" +
			"\n" +
			"/*\n" +
			"// mirror\n" +
			"plane {<-0.5, 0, 1> 10\n" +
			"    texture { Polished_Chrome\n" +
			"        finish { phong 1 }\n" +
			"    }\n" +
			"}\n" +
			"*/\n" +
			"\n" +
			"\n" +
			"// petridish\n" +
			"union {\n" +
			"\n" +
			"    union {\n" +
			"        // bottom\n" +
			"        difference {\n" +
			"            Round_Cylinder ( <0,0,   0>,<0,0.7,0>,  3 , 0.1, 1)\n" +
			"            cylinder { <0,0.05,0>,<0,0.75,0>, 2.95 }\n" +
			"        }\n" +
			"\n" +
			"        // lid\n" +
			"        difference {\n" +
			"            Round_Cylinder ( <0,0.2,0>,<0,0.8,0>, 3.1 , 0.1, 1)\n" +
			"            cylinder { <0,0.1,0>,<0,0.8,0>, 3 }\n" +
			"            rotate<0, 0, -10>\n" +
			"            translate<4.5, 0, 0>\n" +
			"        }\n" +
			"\n" +
			"        texture { Glass3 pigment { rgbf<1.0, 1.0, 1.0, 0.6>}}\n" +
			"        finish  { ambient 0.2 phong 0.7 reflection{ 0.20 metallic 1.00} }\n" +
			"        interior {I_Glass}\n" +
			"    }\n" +
			"\n" +
			"    // water\n" +
			"    cylinder { <0,0.05,0>,<0,0.35,0>, 2.95\n" +
			"        texture { pigment {color rgbt <0.5, 0.7, 1.0, 0.4>} normal {bozo 0.3 scale 0.2 } }\n" +
			"        finish {reflection 0.5}\n" +
			"    }\n" +
			"    rotate<0, -200, 0>\n" +
			"    translate<0,0,3>\n" +
			"}\n" +
			"\n" +
			"#declare Glass_D         = 0.002; \n" +
			"#declare Base_Height     = 0.25; \n" +
			"#declare Base_Half_Width = 0.12; \n" +
			"#declare Neck_Length     = 0.05;\n" +
			"#declare Neck_Radius     = 0.03;\n" +
			"#declare Fillet_Radius   = 0.10; \n" +
			"#declare Base_Border_Radius = 0.025; \n" +
			"//-------------------------------------------------------------------------------------// \n" +
			"#include \"Erlenmeyer_Flask_1.inc\"\n" +
			"//-------------------------------------------------------------------------------------// \n" +
			"object{ Erlenmeyer_Flask_1(  Glass_D, // \n" +
			"                             Base_Height, // Base_H, // \n" +
			"                             Base_Half_Width, // Base_Half_Width, //  \n" +
			"                             Neck_Length, // Neck_Len, // \n" +
			"                             \n" +
			"                             Neck_Radius, // Neck_R, // \n" +
			"                             Fillet_Radius, // Fillet_R, // \n" +
			"                             Base_Border_Radius, // Base_Border_R,//  \n" +
			"                             \n" +
			"                             1, // Merge_On, // 1 for transparent materials \n" +
			"                          ) //-------------------------\n" +
			" \n" +
			"        material{   //-----------------------------------------------------------\n" +
			"         texture { pigment{ rgbf <0.7, 0.8, 0.7, 0.9> }\n" +
			"                   normal { bumps 0.15 scale 0.03} \n" +
			"                   finish { diffuse 0.1 reflection 0.4  \n" +
			"                            specular 0.8 roughness 0.0003 phong 1 phong_size 400}\n" +
			"                 } // end of texture -------------------------------------------\n" +
			"         interior{ ior 1.5 caustics 0.5\n" +
			"                 } // end of interior ------------------------------------------\n" +
			"        } // end of material ----------------------------------------------------\n" +
			"\n" +
			"\n" +
			"        scale <1,1,1>*20\n" +
			"        rotate<0,0,0> \n" +
			"        translate<-3.00, 0, 10.00>}\n" +
			"//---------------------------------------------------------------------------------------\n" +
			"//---------------------------------------------------------------------------------------\n" +
			"\n";

	public static String getMagnifierAt(float x, float y, float z) {
		return
			"// Lupe\n" +
			"union {\n" +
			"    //Lupe\n" +
			"\n" +
			"    // R = sqrt(0.7^2 / (1 - 0.95)^2) = 2.242\n" +
			"    // 0.95 * R = 2.13\n" +
			"    // where 0.7 is the major radius of the torus\n" +
			"    intersection{\n" +
			"        sphere {<-2.13,0,0> 2.242\n" +
			"            texture{Glass }\n" +
			"            interior { ior 1.5 }\n" +
			"\n" +
			"        }\n" +
			"\n" +
			"        sphere {<2.13,0,0> 2.242\n" +
			"            texture{Glass }\n" +
			"            interior { ior 1.5 }\n" +
			"        }\n" +
			"        scale 1\n" +
			"        rotate <0,90,0>\n" +
			"    }\n" +
			"\n" +
			"    torus {0.7, 0.03 // (in the X-Z plane)\n" +
			"        pigment { color Black }\n" +
			"        finish {F_MetalA}\n" +
			"        rotate <90,0,0>\n" +
			"    }\n" +
			"\n" +
			"    cylinder {<0,-0.7,0>, <0,-1.8,0> ,0.08\n" +
			"        pigment { color Black }\n" +
			"        finish {F_MetalA}\n" +
			"        rotate<0, 0, 45>\n" +
			"    }\n" +
			"\n" +
			"    rotate <0, 0, 0>\n" +
			"    translate <" + x + ", " + y + ", " + z + ">\n" +
			"    no_reflection\n" +
			"    // no_image\n" +
			"    no_shadow\n" +
			"}\n";
	}

	public static void main(String[] args) {
		System.out.println(text);
	}
}
