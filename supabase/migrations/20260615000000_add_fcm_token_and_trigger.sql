-- Add fcm_token column to profiles if it doesn't exist
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS fcm_token TEXT;

-- Enable the pg_net extension if you want to make HTTP requests directly from Postgres (optional, but good for webhooks)
CREATE EXTENSION IF NOT EXISTS pg_net;

-- Create the trigger function
CREATE OR REPLACE FUNCTION public.handle_new_booking()
RETURNS TRIGGER AS $$
BEGIN
  -- Call the Supabase Edge Function via HTTP POST
  -- Note: Replace 'your_project_ref' with your actual Supabase project reference
  PERFORM net.http_post(
      url:='https://sqyihnlgozbezeukpgmf.supabase.co/functions/v1/send-notification',
      headers:='{"Content-Type": "application/json", "Authorization": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNxeWlobmxnb3piZXpldWtwZ21mIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODA5Mjc2NzgsImV4cCI6MjA5NjUwMzY3OH0.7ufi6gDkg5i-t8sh4tZI8wfPeFe24uwD4Hrh-CSKcoo"}'::jsonb,
      body:=row_to_json(NEW)::jsonb
  );
  
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Create the trigger on the bookings table
DROP TRIGGER IF EXISTS on_booking_created ON public.bookings;
CREATE TRIGGER on_booking_created
  AFTER INSERT ON public.bookings
  FOR EACH ROW
  EXECUTE FUNCTION public.handle_new_booking();
