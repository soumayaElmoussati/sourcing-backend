package app_sourcing.sellerModule.service;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class ContractService {

    private static final DeviceRgb PRIMARY = new DeviceRgb(16, 185, 129);
    private static final DeviceRgb DARK    = new DeviceRgb(15, 23, 42);
    private static final DeviceRgb MUTED   = new DeviceRgb(100, 116, 139);
    private static final DeviceRgb SURFACE = new DeviceRgb(248, 250, 252);
    private static final DeviceRgb BORDER  = new DeviceRgb(226, 232, 240);
    private static final DeviceRgb BODY_TEXT = new DeviceRgb(51, 65, 85);

    public byte[] generateSignedContract(
            String sellerFullName,
            String sellerEmail,
            String contractNumber,
            LocalDateTime signedAt,
            String signatureBase64
    ) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
        Document doc = new Document(pdf, PageSize.A4);
        doc.setMargins(40, 50, 40, 50);

        PdfFont bold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont italic  = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);

        String dateStr = signedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        // ── HEADER ─────────────────────────────────────────────
        Table header = new Table(UnitValue.createPercentArray(new float[]{1}))
                .useAllAvailableWidth()
                .setBackgroundColor(DARK);

        header.addCell(new Cell()
                .add(new Paragraph("X10ECOM")
                        .setFont(bold).setFontSize(22).setFontColor(PRIMARY)
                        .setTextAlignment(TextAlignment.CENTER).setMarginBottom(4))
                .add(new Paragraph("Fulfillment & Logistics Services Agreement")
                        .setFont(regular).setFontSize(11).setFontColor(ColorConstants.WHITE)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(DARK)
                .setBorder(Border.NO_BORDER)
                .setPadding(20));
        doc.add(header);
        doc.add(new Paragraph("\n").setFontSize(4));

        // ── META (numéro + date) ────────────────────────────────
        Table meta = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth()
                .setBackgroundColor(SURFACE)
                .setBorder(new SolidBorder(BORDER, 1))
                .setMarginBottom(14);

        meta.addCell(metaCell("N° Contrat", contractNumber, bold, regular));
        meta.addCell(metaCell("Date de signature", dateStr, bold, regular));
        doc.add(meta);

        // ── CLAUSES ─────────────────────────────────────────────
        doc.add(sectionTitle("1. Scope of Services / نطاق الخدمات", bold));
        doc.add(bodyParagraph("The Company provides: Product sourcing, Customs clearance, Warehousing, " +
                "Call center and order confirmation, Order Fulfillment (Picking & Packing), Shipping & Delivery, " +
                "Cash on Delivery (COD) Management, Returns Handling, Reporting & Analytics, " +
                "Sending profits to the seller via multiple applications anywhere in the world.", regular));

        doc.add(sectionTitle("2. Call Center / مركز الاتصال", bold));
        doc.add(bodyParagraph("Customers are contacted up to 3 times per day for 3 days (9 attempts). " +
                "If unreachable, the order is cancelled. Upsell and Cross-sell services are available.", regular));

        doc.add(sectionTitle("3. Warehousing / التخزين", bold));
        doc.add(bodyParagraph("Storage is free as long as there are ongoing orders for the products. " +
                "If products are stored in the Company's warehouses in the UAE, orders can be shipped to all GCC countries. " +
                "Inactive goods for more than 60 days may be disposed of.", regular));

        doc.add(sectionTitle("4. Fulfillment & Shipping / التنفيذ والشحن", bold));
        doc.add(bodyParagraph("Orders are processed according to professional standards. " +
                "Shipping is calculated based on actual or volumetric weight (whichever is higher). " +
                "Volumetric formula: (L × W × H) ÷ 5000. Deliveries are made during business days only.", regular));

        doc.add(sectionTitle("5. Delivery & Returns / التوصيل والمرتجعات", bold));
        doc.add(bodyParagraph("Rejected deliveries are counted as delivery attempts. " +
                "Orders are returned to the warehouse. Valid returned items may be restocked after inspection.", regular));

        doc.add(sectionTitle("6. Payments & Remittance / الدفع والتحويلات", bold));
        doc.add(bodyParagraph("Payments are transferred on a weekly basis. Shipping, fees, and commissions " +
                "are deducted before transfer. Bank transfer fees are borne by the client. " +
                "A 2% fee is deducted from each client withdrawal or payout.", regular));

        doc.add(sectionTitle("7. International Shipping / الشحن الدولي", bold));
        doc.add(bodyParagraph("Prices exclude VAT, customs duties, and government fees. " +
                "All such costs are borne by the client.", regular));

        doc.add(sectionTitle("8. Liability / المسؤولية", bold));
        doc.add(bodyParagraph("The client is responsible for product legality and quality. " +
                "Shipments are protected in the warehouse, and any damage is compensated based on the declared value.", regular));

        doc.add(sectionTitle("9. Fraud Protection / الحماية من الاحتيال", bold));
        doc.add(bodyParagraph("The company may suspend suspicious accounts or orders. " +
                "Payments may be held in case of suspected fraud.", regular));

        doc.add(sectionTitle("10. Confidentiality / السرية", bold));
        doc.add(bodyParagraph("All data and information are confidential and will not be shared with any third party.", regular));

        doc.add(sectionTitle("11. Contract Duration / مدة العقد", bold));
        doc.add(bodyParagraph("The contract is valid for one year. It is automatically renewed unless terminated with prior notice.", regular));

        doc.add(sectionTitle("12. Governing Law / القانون", bold));
        doc.add(bodyParagraph("This Agreement is governed by applicable laws in the operating jurisdiction.", regular));

        doc.add(sectionTitle("13. Acceptance / القبول", bold));
        doc.add(bodyParagraph("By using X10ECOM services, the client agrees to all terms and conditions stated in this agreement.", regular));

        // ── SIGNATURE BLOCK ─────────────────────────────────────
        doc.add(new Paragraph("\n").setFontSize(6));

        Table sigBlock = new Table(UnitValue.createPercentArray(new float[]{1}))
                .useAllAvailableWidth()
                .setBorder(new SolidBorder(PRIMARY, 1.5f))
                .setBackgroundColor(SURFACE)
                .setMarginTop(10);

        // Header de la signature
        sigBlock.addCell(new Cell()
                .add(new Paragraph("✍  Signature / التوقيع")
                        .setFont(bold).setFontSize(12).setFontColor(PRIMARY))
                .setBackgroundColor(new DeviceRgb(240, 253, 244))
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(BORDER, 1))
                .setPadding(12));

        // Grille infos seller
        Table infoGrid = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth();
        infoGrid.addCell(sigInfoCell("Client Name / الاسم", sellerFullName, bold, regular));
        infoGrid.addCell(sigInfoCell("Email", sellerEmail, bold, regular));
        infoGrid.addCell(sigInfoCell("Contract N°", contractNumber, bold, regular));
        infoGrid.addCell(sigInfoCell("Date", dateStr, bold, regular));

        sigBlock.addCell(new Cell()
                .add(infoGrid)
                .setBorder(Border.NO_BORDER)
                .setPadding(12));

        // Image de la signature
        if (signatureBase64 != null && !signatureBase64.isBlank()) {
            String b64 = signatureBase64.contains(",")
                    ? signatureBase64.split(",")[1]
                    : signatureBase64;
            byte[] sigBytes = Base64.getDecoder().decode(b64);

            Image sigImg = new Image(ImageDataFactory.create(sigBytes))
                    .setMaxWidth(200)
                    .setMaxHeight(80)
                    .setHorizontalAlignment(HorizontalAlignment.LEFT);

            sigBlock.addCell(new Cell()
                    .add(new Paragraph("Signature :")
                            .setFont(bold).setFontSize(9).setFontColor(MUTED))
                    .add(sigImg)
                    .setBorder(Border.NO_BORDER)
                    .setPadding(12)
                    .setPaddingTop(4));
        }

        doc.add(sigBlock);

        // ── FOOTER ──────────────────────────────────────────────
        doc.add(new Paragraph("\n").setFontSize(8));
        doc.add(new Paragraph(
                "Ce contrat a été signé électroniquement le " + dateStr +
                        " et est juridiquement contraignant conformément aux lois applicables sur la signature électronique.")
                .setFont(italic).setFontSize(8).setFontColor(MUTED)
                .setTextAlignment(TextAlignment.CENTER));
        doc.add(new Paragraph("© 2026 X10ECOM — Tous droits réservés")
                .setFont(regular).setFontSize(8).setFontColor(MUTED)
                .setTextAlignment(TextAlignment.CENTER));

        doc.close();
        return baos.toByteArray();
    }

    // ── Helpers ───────────────────────────────────────────────
    private Paragraph sectionTitle(String text, PdfFont bold) {
        return new Paragraph(text)
                .setFont(bold).setFontSize(10).setFontColor(PRIMARY)
                .setMarginTop(10).setMarginBottom(3)
                .setBorderBottom(new SolidBorder(PRIMARY, 0.5f));
    }

    private Paragraph bodyParagraph(String text, PdfFont regular) {
        return new Paragraph(text)
                .setFont(regular).setFontSize(9).setFontColor(BODY_TEXT)
                .setMarginBottom(4).setMultipliedLeading(1.4f);
    }

    private Cell metaCell(String label, String value, PdfFont bold, PdfFont regular) {
        return new Cell()
                .add(new Paragraph(label)
                        .setFont(bold).setFontSize(8).setFontColor(MUTED).setMarginBottom(2))
                .add(new Paragraph(value)
                        .setFont(bold).setFontSize(10).setFontColor(DARK))
                .setBorder(Border.NO_BORDER)
                .setPadding(10);
    }

    private Cell sigInfoCell(String label, String value, PdfFont bold, PdfFont regular) {
        return new Cell()
                .add(new Paragraph(label)
                        .setFont(bold).setFontSize(8).setFontColor(MUTED).setMarginBottom(1))
                .add(new Paragraph(value)
                        .setFont(regular).setFontSize(10).setFontColor(DARK))
                .setBorder(Border.NO_BORDER)
                .setPaddingBottom(6);
    }
}