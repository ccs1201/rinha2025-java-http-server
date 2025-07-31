package br.com.ccs.rinha.model.input;

public final class PaymentRequest {
    public String correlationId;
    public int amount;
    public long requestedAt;
    public boolean isDefault;

    public String json;

    public void setDefaultFalse() {
        this.isDefault = false;
    }

    public void setDefaultTrue() {
        this.isDefault = true;
    }

    public String getJson() {
        return json;
    }

}