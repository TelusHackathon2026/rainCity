import gradio as gr
from ultralytics import YOLO
from PIL import Image
import numpy as np
import io
import base64

# Load model
model = YOLO("best.pt")

def detect_hazards(image):
    # Run Inference
    results = model(image, conf=0.25)
    result = results[0]  # Get results for the single image

    # 1. Generate the labeled image
    # plot() draws the boxes and labels and returns a numpy array
    labeled_img_array = result.plot() 
    labeled_img = Image.fromarray(labeled_img_array.astype('uint8'))

    # 2. Generate the JSON data
    summary = {}
    for box in result.boxes:
        cls_name = result.names[int(box.cls[0])]
        conf = float(box.conf[0])
        # Only keep the highest confidence for each label type
        if cls_name not in summary or conf > summary[cls_name]["confidence"]:
            summary[cls_name] = {
                "label": cls_name,
                "confidence": round(conf, 3)
            }

    # Convert map to a simple list
    return labeled_img, list(summary.values())

# Define the interface with TWO outputs
demo = gr.Interface(
    fn=detect_hazards, 
    inputs=gr.Image(type="pil"), 
    outputs=[gr.Image(type="pil", label="Labeled Image"), gr.JSON(label="Detection Data")]
)

demo.queue().launch()
