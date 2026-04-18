from fastapi import FastAPI, Request, HTTPException
import httpx
import os
from dotenv import load_dotenv

load_dotenv()

app = FastAPI()

# Pour ton Supabase local (Docker)
SUPABASE_URL = "http://localhost:54321" # Port par défaut de Supabase local
SUPABASE_KEY = "TON_SERVICE_ROLE_KEY"

@app.get("/health")
async def health_check():
    return {"status": "FastAPI is linked to Local Supabase"}

# Exemple : Valider un score et le sauvegarder
@app.post("/api/v1/submit-score")
async def submit_score(data: dict):
    # Logique métier : par exemple, vérifier si le score est cohérent
    score = data.get("score")
    user_id = data.get("user_id")
    
    if score > 30: # Protection contre la triche
        raise HTTPException(status_code=400, detail="Score invalide")

    async with httpx.AsyncClient() as client:
        # On relaie vers Supabase local
        response = await client.patch(
            f"{SUPABASE_URL}/rest/v1/profiles?id=eq.{user_id}",
            json={"best_score": score},
            headers={
                "apikey": SUPABASE_KEY,
                "Authorization": f"Bearer {SUPABASE_KEY}",
                "Content-Type": "application/json"
            }
        )
    return {"status": "success", "supabase_response": response.status_code}