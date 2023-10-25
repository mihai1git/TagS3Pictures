# Tag Schema

#### Version 3_05.08.2023_9tags

| Tag    												| Possible values 							|
| -------- 												| ------- 									|
|personal:data:content:has_faces	 					| 1,2,3,4,5_max								|
|personal:data:content:has_faces:is_me 					| true										|
|personal:data:content:has_faces:is_me:face_occluded	| true										|
|personal:data:content:has_text							| true										|
|personal:data:content:labels:has_persons				| 1,2,3,...									|
|personal:data:content:labels:has_landmarks				| true										|
|personal:data:content:labels:dominant_category			| [AWS Rekognition category: 41 categories]	|
|personal:data:content:labels:dominant_label	 		| [AWS Rekognition label: 3000+ labels]		|
|personal:schema:version								| 3_05.08.2023_9tags						|

#### Version 2_26.07.2023

| Tag    												| Possible values 							|
| -------- 												| ------- 									|
|personal:data:content:has_faces	 					| 0,1,2,3,4,5_max							|
|personal:data:content:has_faces:is_me 					| true/false								|
|personal:data:content:has_faces:is_me:face_occluded	| true/false								|
|personal:data:content:has_text							| true/false								|
|personal:data:content:has_text:language				| ro/en/...									|
|personal:data:content:labels:has_persons				| 0,1,2,3,...								|
|personal:data:content:labels:has_landmarks				| true/false								|
|personal:data:content:labels:dominant_category			| [AWS Rekognition category: 41 categories]	|
|personal:data:content:labels:dominant_label	 		| [AWS Rekognition label: 3000+ labels]		|
|personal:schema:version								| 2_26.07.2023								|

#### Version 1_25.07.2023

| Tag    												| Possible values 							|
| -------- 												| ------- 									|
|personal:data:content:has_faces	 					| 0,1,2,3,4,5_max							|
|personal:data:content:has_faces:is_me 					| true/false								|
|personal:data:content:has_faces:is_me:with_glasses		| true/false								|
|personal:data:content:has_text							| true/false								|
|personal:data:content:has_text:language				| ro/en/...									|
|personal:data:content:labels:has_persons				| 0,1,2,3,...								|
|personal:data:content:labels:has_landmarks				| true/false								|
|personal:data:content:labels:dominant_category			| [AWS Rekognition category: 41 categories]	|
|personal:data:content:labels:dominant_label	 		| [AWS Rekognition label: 3000+ labels]		|
|personal:schema:version								| 2_26.07.2023								|

#### Version 0_10.06.2023

| Tag    												| Possible values 							|
| -------- 												| ------- 									|
|personal:data:content:has_faces	 					| true/false								|
|personal:owner 										| mihaiadam									|
|personal:data:privacy 									| restricted								|
