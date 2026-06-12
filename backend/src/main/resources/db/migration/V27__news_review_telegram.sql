-- News Engine S2: revisión editorial vía Telegram.
--
-- telegram_message_id marca que la solicitud de revisión YA se envió al chat
-- (NULL = pendiente de enviar/reenviar) y permite editar ese mensaje al
-- resolverla. El estado nuevo DISCARDED no necesita DDL: la columna status es
-- varchar(20) sin CHECK constraint desde V22.
ALTER TABLE public.news_item
    ADD COLUMN IF NOT EXISTS telegram_message_id BIGINT;
