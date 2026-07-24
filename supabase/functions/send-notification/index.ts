import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { google } from "https://esm.sh/googleapis@105"

// Note: Ensure you upload your service account json string to the Supabase Edge Function Secrets
// e.g., supabase secrets set FIREBASE_SERVICE_ACCOUNT="your_json_string_here"

serve(async (req) => {
  try {
    const payload = await req.json()
    console.log("Webhook payload received: ", payload)

    // The payload.record represents the newly inserted booking
    const booking = payload.record
    if (!booking || !booking.user_id) {
      throw new Error("Invalid payload: missing booking or user_id")
    }

    // 1. Initialize Supabase Admin Client
    const supabaseUrl = Deno.env.get('SUPABASE_URL')!
    const supabaseKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
    const supabase = createClient(supabaseUrl, supabaseKey)

    // 2. Fetch the user's FCM token from profiles table
    const { data: profileData, error: profileError } = await supabase
      .from('profiles')
      .select('fcm_token')
      .eq('id', booking.user_id)
      .single()

    if (profileError || !profileData || !profileData.fcm_token) {
      console.error("FCM Token not found for user: ", booking.user_id)
      return new Response(JSON.stringify({ error: "FCM Token not found" }), { status: 400 })
    }

    const fcmToken = profileData.fcm_token

    // 3. Prepare Notification Content
    const title = "Booking Confirmed!"
    const body = `Your appointment for ${booking.appointment_time} is confirmed. Amount: PKR ${booking.total_amount}`

    // 4. Authenticate with Firebase via Google APIs (HTTP v1)
    const serviceAccountJson = Deno.env.get('FIREBASE_SERVICE_ACCOUNT')
    if (!serviceAccountJson) {
      throw new Error("FIREBASE_SERVICE_ACCOUNT secret is missing")
    }
    const serviceAccount = JSON.parse(serviceAccountJson)

    const jwtClient = new google.auth.JWT(
      serviceAccount.client_email,
      null,
      serviceAccount.private_key,
      ['https://www.googleapis.com/auth/cloud-platform']
    )

    const tokens = await jwtClient.authorize()
    const accessToken = tokens.access_token

    // 5. Send the Push Notification via FCM HTTP v1 API
    const projectId = serviceAccount.project_id
    const fcmSendUrl = `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`

    const fcmMessage = {
      message: {
        token: fcmToken,
        notification: {
          title: title,
          body: body
        },
        data: {
          booking_id: booking.id,
          click_action: "FLUTTER_NOTIFICATION_CLICK"
        }
      }
    }

    const fcmResponse = await fetch(fcmSendUrl, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(fcmMessage)
    })

    const fcmResult = await fcmResponse.json()
    console.log("FCM Response: ", fcmResult)

    return new Response(JSON.stringify({ success: true, result: fcmResult }), {
      headers: { "Content-Type": "application/json" },
    })

  } catch (err) {
    console.error("Function error: ", err)
    return new Response(JSON.stringify({ error: err.message }), { status: 500 })
  }
})