3Dscript
========
3Dscript is a plugin for Fiji/ImageJ for creating 3D and 4D animations of microscope data. In contrast to existing 3D visualization packages, animations are not keyframe-based, but are described by a natural language-based syntax.

Find more information at https://bene51.github.io/3Dscript

Requirements:
-------------
* 64-bit Operating System (Linux, Mac OS X or Windows)
* Up-to-date Fiji installation (http://www.fiji.sc)
* OpenCL 1.2-capable Graphics Card or better
* Decent graphics card, preferably from Nvidia or AMD, but newer Intel cards will work also (see below for tested hardware)

Installation:
-------------
* Start Fiji
* Click on Help>Update...
* Click on "Manage update sites"
* Check the box in front of "3Dscript"
* Click on "Close"
* Click on "Apply changes"
* Restart Fiji
* Installation typically takes not longer than a couple of minutes

Demo:
-----
* Start Fiji
* Click on File>Open Samples>T1 Head
* Click on Plugins>3D script>Interactive Animation
* In the "Interactive Raycaster" window, click on "show" next to "Animation"
* Click on "Start text-based animation editor"
* In the editor window, type the following text:
  From frame 0 to frame 200 rotate by 360 degrees horizontally
* Click on "Run"
  This will render 200 frames of a movie sequence, within which the MRI data set rotates by 360 degrees.

Rendering of 200 frames of this data set will typically take less than a minute on an OpenCL-enabled Graphics Card.
The resulting stack can be saved as a video file using Fiji's File>Save As>AVI... command.

To run the software on another data set, open a different image stack (instead of the T1 Head sample data) before running 3Dscript.

More information is available at https://bene51.github.io/3Dscript

Usage:
------
Detailed usage information is available on the [wiki](https://github.com/bene51/3Dscript/wiki).

Tested hardware:
----------------
Graphics card | Operating system
------------- | -------------
NVIDIA Quadro P2000 | Windows 10
NVIDIA Quadro M4000 | Windows 10
NVIDIA Quadro M2000 | Windows 10
NVIDIA Quadro K620  | Windows 10
NVIDIA Quadro K600  | Windows 10
NVIDIA Quadro K420  | Windows 7
NVIDIA GeForce GTX760 | Windows 7
AMD FirePro W5100   | Windows 7
Intel HD Graphics 4600 | Windows 7
Intel HD Graphics 4000 | Mac OS X
Intel HD Graphics 5000 | Mac OS X
Intel HD Graphics 530 | Windows 10

