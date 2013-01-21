The emulator uses the following packet structure (derived from AP1.2 of FoG documents):

 Layer 2a (Ehernet):
   	entry			            | 	size
   	-------------------------------------------------	
   	Destination MAC address     |	6 bytes
   	Source MAC address	        |	6 bytes
   	Ether type		            |	2 bytes = "0x606"
   	
 Layer 2b (FoG LLC):
   	entry			            | 	size
   	-------------------------------------------------	
   	Fragmentation flags         |	1 byte
   	
 Layer 3 (FoG):
   	entry		             	| 	size
   	-------------------------------------------------	
	Header length		        |	2 bytes	    	
	Markings		            |	2 bytes
	Loop counter 		        | 	2 bytes
	Payload length		        | 	4 bytes
	Route length		        | 	4 bytes
	Route			            | 	>= 1 bytes / variable	

 Ether type:
  The range between 1501 and 1535 is undefined in standard, values <= 1500 are reserved for backward compatibility to Ethernet I and represent the packet size.
  A value below 0x600 (1536D) indicates an Ethernet I packet. We use 0x606 as ether type, which is unused till now. The lowest used value is 0x800, which is
  used for the Internet Protocol v4.