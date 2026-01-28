<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">

<a name="readme-top"></a>

<div class="center">
  <a href="#">
  </a>
  <h1>Predictive Traffic & Crime Monitoring System</h1>
  <p>
    A real-time traffic and hazard detection platform using YOLOv11, React, and Spring Boot for monitoring city intersections and alerting authorities.
  </p>
</div>

<h2>About The Project</h2>
<p>
  Urban areas face increasing traffic congestion and safety hazards. This system was designed to predict potential accidents, fallen trees, and other hazards in real time to assist authorities and improve city safety.
</p>
<ul>
  <li><strong>Motivation:</strong> Automating hazard detection to reduce emergency response times.</li>
  <li><strong>Solution:</strong> A full-stack application that analyzes live camera feeds, detects hazards using AI, calculates a hazard score, and provides actionable insights through an interactive dashboard.</li>
</ul>

<h3>Tech Stack</h3>
<div class="center">
  <img src="https://img.shields.io/badge/react-%2320232a.svg?style=for-the-badge&logo=react&logoColor=%2361DAFB" class="badge">
  <img src="https://img.shields.io/badge/springboot-%236DB33F.svg?style=for-the-badge&logo=springboot&logoColor=white" class="badge">
  <img src="https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=java&logoColor=white" class="badge">
  <img src="https://img.shields.io/badge/python-%233776AB.svg?style=for-the-badge&logo=python&logoColor=white" class="badge">
  <img src="https://img.shields.io/badge/YOLOv11-%23FF5733.svg?style=for-the-badge" class="badge">
  <img src="https://img.shields.io/badge/tailwindcss-%2338B2AC.svg?style=for-the-badge&logo=tailwind-css&logoColor=white" class="badge">
  <img src="https://img.shields.io/badge/supabase-%2300F1A1.svg?style=for-the-badge&logo=supabase&logoColor=white" class="badge">
  <img src="https://img.shields.io/badge/WebSockets-%233399FF.svg?style=for-the-badge" class="badge">
  <img src="https://img.shields.io/badge/DeepSeek-AI-blueviolet?style=for-the-badge" class="badge">
</div>

<hr>

<h2>Setup Instructions</h2>

<h3>1. Clone the repository</h3>
<pre><code>git clone https://github.com/yourusername/rainCity.git
cd rainCity
</code></pre>

<h3>2. Backend Setup (Spring Boot + Java)</h3>
<pre><code>cd backend
./mvnw spring-boot:run
</code></pre>
<ul>
  <li><strong>Prerequisites:</strong> Ensure <strong>Java 17+</strong> is installed.</li>
  <li><strong>Environment:</strong> Configure <code>application.yml</code> for API keys and database URL.</li>
<h3>Configuration Example (application.yml)</h3>
<pre><code class="yaml">
spring:
  application:
    name: rainCity-hazard-detector
server:
  port: 8080
app:
  supabase:
    url: https://your-supabase-project.supabase.co/
    rest-url: ${app.supabase.url}/rest/v1
    anon-key: YOUR_SUPABASE_ANON_KEY
  ai-model:
    hf-api-url: https://your-hf-space.hf.space/api/predict
    hf-token: YOUR_HF_API_TOKEN
  deepseek-api:
    url: https://your-deepseek-api.example.com/
    access-token: YOUR_DEEPSEEK_ACCESS_TOKEN
hazard:
  refresh-rate-ms: 10000
</code></pre>


  
</ul>

<h3>3. Frontend Setup (React + TailwindCSS)</h3>
<pre><code>cd frontend
npm install
npm run dev
</code></pre>

<h3>4. YOLOv11 Model Integration</h3>
<ul>
  <li>Place your trained YOLOv11 <code>.pt</code> model in <code>backend/models/</code> or use ours:</li>
  <a href="https://huggingface.co/spaces/sdl11/intersection-hazard-api" target="_blank" rel="noopener noreferrer">
    Intersection Hazard API
  </a>
  <li>Ensure the model path matches your <code>application.properties</code> configuration.</li>
  <li>The backend serves predictions via REST API or WebSocket stream.</li>
</ul>

<h3>5. Database & Monitoring</h3>
<ul>
  <li><strong>Supabase:</strong> Used for storing hazard logs and historical data. Configure keys in <code>.env</code>.</li>
  <li><strong>WebSockets:</strong> Handles live camera feed updates and real-time hazard score alerts.</li>
</ul>

<hr>

<h2>Roadmap</h2>
<ul>
  <li>Train YOLOv11 model for hazard detection</li>
  <li>Connect backend to live traffic camera feeds</li>
  <li> Implement real-time hazard scoring logic</li>
  <li> Integrate Supabase for historical analysis</li>
  <li> Build React dashboard to display alerts</li>
  <li> Implement WebSocket updates for live monitoring</li>
  <li> AI-generated descriptions for detected hazards</li>
  <li> Dataset Expansion and Optimization</li>
  <li> Interactive Map Feature</li>
  <li> Multi City Expansion</li>
</ul>

<p class="center"><a href="#readme-top">back to top</a></p>

</body>
</html>
