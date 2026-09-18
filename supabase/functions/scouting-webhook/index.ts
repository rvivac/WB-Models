// Follow this setup guide to integrate the Deno language server with your editor:
// https://deno.land/manual/getting_started/setup_your_environment
// This code runs on Supabase Edge Functions (Deno runtime)

import { serve } from 'https://deno.land/std@0.168.0/http/server.ts';

const N8N_SCOUTING_WEBHOOK_URL = Deno.env.get('N8N_SCOUTING_WEBHOOK_URL') || '';
const WEBHOOK_SECRET_KEY = Deno.env.get('WEBHOOK_SECRET_KEY') || '';

interface ScoutingWebhookPayload {
  type: 'INSERT' | 'UPDATE';
  table: 'candidatures';
  record: {
    id: string;
    full_name: string;
    email: string;
    phone_whatsapp: string;
    birth_date: string;
    city: string;
    state: string;
    gender: string;
    height_cm: number;
    instagram?: string;
    uploaded_photos: Array<{ url: string; label: string }>;
    lgpd_consent_given: boolean;
    created_at: string;
  };
}

serve(async (req) => {
  // Verificação de método
  if (req.method !== 'POST') {
    return new Response(JSON.stringify({ error: 'Method not allowed' }), {
      status: 405,
      headers: { 'Content-Type': 'application/json' },
    });
  }

  try {
    // Validação de Secret no Header para chamadas internas do Supabase Database Webhook
    const authHeader = req.headers.get('x-webhook-secret');
    if (WEBHOOK_SECRET_KEY && authHeader !== WEBHOOK_SECRET_KEY) {
      return new Response(JSON.stringify({ error: 'Unauthorized webhook call' }), {
        status: 401,
        headers: { 'Content-Type': 'application/json' },
      });
    }

    const payload: ScoutingWebhookPayload = await req.json();

    // Validação básica do registro recebido
    if (payload.table !== 'candidatures' || !payload.record) {
      return new Response(JSON.stringify({ message: 'Ignored: not a candidature record' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      });
    }

    // Encaminhamento assíncrono para automação no N8N (Disparo de WhatsApp para booker, notificação Slack/Telegram)
    if (N8N_SCOUTING_WEBHOOK_URL) {
      const n8nResponse = await fetch(N8N_SCOUTING_WEBHOOK_URL, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Origin-Agency': 'WB-Scouting',
        },
        body: JSON.stringify({
          event: 'NEW_TALENT_APPLICATION',
          candidate: {
            id: payload.record.id,
            name: payload.record.full_name,
            whatsapp: payload.record.phone_whatsapp,
            cityState: `${payload.record.city}/${payload.record.state}`,
            heightCm: payload.record.height_cm,
            gender: payload.record.gender,
            instagram: payload.record.instagram || 'N/A',
            photoCount: payload.record.uploaded_photos?.length || 0,
            submittedAt: payload.record.created_at,
          },
        }),
      });

      if (!n8nResponse.ok) {
        console.error('Falha ao encaminhar dados para N8N:', await n8nResponse.text());
      }
    }

    return new Response(
      JSON.stringify({
        success: true,
        candidate_id: payload.record.id,
        status: 'Processed & Dispatched',
      }),
      {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }
    );
  } catch (error: any) {
    return new Response(
      JSON.stringify({ error: error.message || 'Internal Server Error' }),
      {
        status: 500,
        headers: { 'Content-Type': 'application/json' },
      }
    );
  }
});
