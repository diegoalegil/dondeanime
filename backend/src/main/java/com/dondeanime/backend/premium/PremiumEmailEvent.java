package com.dondeanime.backend.premium;

/**
 * Email transaccional de Premium pendiente de envío. Se publica DENTRO de la
 * tx del webhook de Stripe y se consume tras el commit (ver
 * {@link PremiumEmailEventListener}): así un fallo de Resend no hace rollback
 * del alta Premium de un cliente que ya pagó.
 */
public record PremiumEmailEvent(
        Type type,
        String email,
        String planTier,
        String paidAt,
        String manageUrl) {

    public enum Type {
        WELCOME, RECEIPT
    }

    public static PremiumEmailEvent welcome(String email, String planTier, String manageUrl) {
        return new PremiumEmailEvent(Type.WELCOME, email, planTier, null, manageUrl);
    }

    public static PremiumEmailEvent receipt(String email, String planTier, String paidAt, String manageUrl) {
        return new PremiumEmailEvent(Type.RECEIPT, email, planTier, paidAt, manageUrl);
    }
}
