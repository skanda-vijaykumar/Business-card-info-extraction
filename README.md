## **Business Card Info Extraction**  

An end-to-end solution for detecting business cards in images and extracting structured contact information using Visual Language Models (VLMs). The project demonstrates the superiority of transformer-based architectures over traditional convolutional neural networks for feature recognition and information extraction. This project is unique cause it works completely locally and is useful for companies who was to reduce redundant work and yet keep data secure.  

This is a simple project. The only point was for me to help automate redundant data entry stuff. You can modify and essestially build a system to extract anything from anything. VLMs seem better for unknown data than using and training CNN's or other OCR + object detection methods. 

A little bit of tinkering and I think this project could help out people who are worried about data.

## Demo  
Left  is the server side, right is the android app

![temp](https://github.com/user-attachments/assets/1d0e665a-eef1-4bf1-af45-700bf3b0cda2)




## Features  
- **Data stored locally:** The entire system is run local network. Your image and data is stored on your hardware only.  
- **Card Detection:** Locates and crops business cards from arbitrary photographs.  
- **Multi-Modal Extraction:** Leverages VLMs to jointly process visual layouts and text, extracting fields such as name, company, designation, phone number, email, and address. (can be customized by just changing the prompt in the back-end) 
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
1. Run the python script at the server end. (main.py)
2. Open the app.
3. Enter the IP address of your server (1st time only)
4. Click an image of the business card using the app.
5. BOOM! image is sent to the server >> structured information is extracted >> extracted information is shown on your phone app. 
6. At the server end information is stored in .xlsx and .json formats. Info is also saved in logs section of the app.
