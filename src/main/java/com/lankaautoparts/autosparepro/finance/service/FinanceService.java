package com.lankaautoparts.autosparepro.finance.service;

import com.lankaautoparts.autosparepro.finance.model.Transaction;
import com.lankaautoparts.autosparepro.finance.repository.TransactionRepository;
import com.lankaautoparts.autosparepro.payment.model.Payment;
import com.lankaautoparts.autosparepro.payment.model.PaymentStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class FinanceService {
    private final TransactionRepository transactionRepository;

    public FinanceService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id).orElse(new Transaction());
    }

    public Transaction saveTransaction(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    public void deleteTransaction(Long id) {
        transactionRepository.deleteById(id);
    }

    public double getNetTotal() {
        return transactionRepository.sumAmount();
    }

    public long countBySource(String source) {
        return transactionRepository.countBySource(source);
    }

    /**
     * Records a Finance transaction from a completed payment, so Finance is
     * no longer a disconnected manual table. Idempotent — safe to call more
     * than once for the same payment (e.g. once when the payment is first
     * made, and again if an admin later edits it back to COMPLETED).
     */
    public void recordFromPayment(Payment payment) {
        if (payment == null || payment.getStatus() != PaymentStatus.COMPLETED || payment.getInvoiceNumber() == null) {
            return;
        }
        if (transactionRepository.existsBySourceAndInvoiceReference("AUTO", payment.getInvoiceNumber())) {
            return;
        }

        Transaction t = new Transaction();
        t.setInvoiceReference(payment.getInvoiceNumber());
        t.setAmount(payment.getAmount());
        t.setPaymentMethod(payment.getPaymentMethod() + " (" + payment.getCardType() + ")");
        t.setDate(payment.getPaymentDate() != null ? payment.getPaymentDate() : LocalDate.now());
        t.setSource("AUTO");
        transactionRepository.save(t);
    }

    /**
     * Records the money-out side of a refund against the ledger. Idempotent:
     * a payment can only ever be refunded once, so calling this twice for the
     * same payment never books the money out twice.
     */
    public void recordRefund(Payment payment) {
        if (payment == null || payment.getInvoiceNumber() == null) {
            return;
        }
        String reference = payment.getInvoiceNumber() + "-REFUND";
        if (transactionRepository.existsBySourceAndInvoiceReference("AUTO", reference)) {
            return;
        }
        Transaction t = new Transaction();
        t.setInvoiceReference(reference);
        t.setAmount(-Math.abs(payment.getAmount()));
        t.setPaymentMethod("Refund");
        t.setDate(LocalDate.now());
        t.setSource("AUTO");
        transactionRepository.save(t);
    }
}
