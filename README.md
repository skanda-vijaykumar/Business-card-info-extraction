**Business Card Info Extraction**  

An end-to-end solution for detecting business cards in images and extracting structured contact information using Visual Language Models (VLMs). The project demonstrates the superiority of transformer-based architectures over traditional convolutional neural networks for feature recognition and information extraction.[1]

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
- JSON, pandas for output serialization  

## Installation  
```bash
git clone https://github.com/skanda-vijaykumar/Business-card-info-extraction.git
cd Business-card-info-extraction
pip install -r requirements.txt
```

## Usage  
1. Place your test images in the `images/` directory.  
2. Run the extraction script:  
   ```bash
   python extract_info.py --input images/card1.jpg --output results.json
   ```
3. View structured fields in `results.json` or load into a pandas DataFrame for analysis.  

## Demo  
A demo video illustrating detection and extraction can be viewed from the project’s GitHub page.[1]  

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

---
Answer from Perplexity: pplx.ai/share
