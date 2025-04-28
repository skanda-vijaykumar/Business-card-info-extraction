## **Business Card Info Extraction**  

An end-to-end solution for detecting business cards in images and extracting structured contact information using Visual Language Models (VLMs). The project demonstrates the superiority of transformer-based architectures over traditional convolutional neural networks for feature recognition and information extraction. This project is unique cause it works completely locally and is useful for companies who was to reduce redundant work and yet keep data secure.  

## Features  
- **Card Detection:** Locates and crops business cards from arbitrary photographs.  
- **Multi-Modal Extraction:** Leverages VLMs to jointly process visual layouts and text, extracting fields such as name, company, designation, phone number, email, and address.  
- **Structured Output:** Returns results in JSON or CSV format for easy integration with CRMs or databases.  
- **Demo Interface:** Includes a simple web or notebook-based demo for quick experimentation.  

## Motivation  
Traditional OCR pipelines often struggle with varied card designs, fonts, and layouts. By using transformer-based VLMs, this project aims to:  
- Improve accuracy across diverse card styles  
- Reduce reliance on manual tuning of layout analysis  
- Leverage pre-trained multi-modal models for robust feature recognition  

## Architecture  
1. **Preprocessing:** Input images are resized and normalized.  
2. **Card Localization:** A vision transformer or object detection model identifies the card region.  
3. **Feature Encoding:** The cropped card is fed into a VLM that encodes visual regions and tokenized text jointly.  
4. **Entity Extraction:** A classification head predicts field labels (e.g., NAME, PHONE, EMAIL) for each text token.  
5. **Postprocessing:** Predicted tokens are assembled into structured records.  

## Requirements  
- Python 3.8+  
- PyTorch or TensorFlow  
- Transformers library (e.g., Hugging Face)  
- OpenCV, PIL for image handling
- Ollama  
- JSON, pandas for output serialization  
- Android studio
  
## Installation
Create a virtual env and install necessary packages. I think I have included all relevant ones. mb if I missed out on anything.

```bash
git clone https://github.com/skanda-vijaykumar/Business-card-info-extraction.git
cd Business-card-info-extraction
pip install -r requirements.txt
```
Open andriod studio 'cd' to cloned repo and load the project directry which is named 'CardDetector' >> Clean gradel and build gradel >> run the app file. 
Great! Now you have built the mobile app.

now in this repo I have shared the main.py file for the back end. 
run this script on your server or hardware. This has the VLM which receives image,processes the image and extracts the data and stores the image in an .xlsx and json file. 
 
## Usage  
1. Open the app. Click an image of the business card.
2. BOOM! image is sent to the server >> information is extracted >> extracted information is shown on your phone app. 
3. You can check the logs.
   
## Demo  
[A demo video]! https://github.com/skanda-vijaykumar/Business-card-info-extraction/Card_detection_final.mp4

## Contributing  
- Fork the repository and create a feature branch for new functionalities.  
- Ensure code follows project linting and formatting standards.  
- Add unit tests for new modules and maintain coverage.  
- Submit a pull request with a detailed description of changes.  

## License  
This project is released under the MIT License.  

## Acknowledgments  
- The Hugging Face Transformers team for VLM architectures.  
- OpenCV and PIL communities for image processing utilities.  
- Inspiration from existing OCR and business-card extraction tools.

Citations:
[1] https://github.com/skanda-vijaykumar/Business-card-info-extraction
[2] https://github.com/tulasinnd/Text-Extraction-From-Business-Card-Using-OCR
[3] https://github.com/bhavyabhagerathi/BizCardX-Extracting-Business-Card-Data-with-OCR
[4] https://github.com/Go7bi/BizCardX_-Extracting-Business-Card-Data-with-OCR
[5] https://github.com/Muthukumar0908/BizCardX-Extracting-Business-Card-Data-with-OCR/blob/main/README.md
[6] https://github.com/DineshDhamodharan24/BizCardX-Extracting-Business-Card-Data-with-OCR
[7] https://github.com/Thiruvenkatam007/business-card-data-extraction/blob/main/README.md
[8] https://arxiv.org/pdf/2403.05530.pdf
[9] https://www.scribd.com/document/718355041/Gemini-1-5-Report
[10] https://arxiv.org/html/2403.05530v2
