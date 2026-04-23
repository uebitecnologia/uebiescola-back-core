-- Inscrição Municipal da escola (obrigatoria para emissao de NFS-e via Asaas)
ALTER TABLE schools
    ADD COLUMN IF NOT EXISTS municipal_registration VARCHAR(40);
