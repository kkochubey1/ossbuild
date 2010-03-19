

Information
-----------------------------------------------

The RXTX () source has been modified from its original form. It uses 
JNA's NativeLibrary.getInstance() instead of System.loadLibrary() because the 
OSSBuild resource package management system relies on the fact that the system 
PATH can be set at runtime. Java and System.loadLibrary() use the system 
property "java.library.path" which is set at the time the JVM (process) is 
started. Modifying it using reflection was considered but rejected as a very 
undesirable and incompatible solution.