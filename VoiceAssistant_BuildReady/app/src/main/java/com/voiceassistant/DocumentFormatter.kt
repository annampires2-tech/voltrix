package com.voiceassistant

import android.content.Context
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.ParagraphAlignment
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.List
import com.itextpdf.layout.element.ListItem
import com.itextpdf.layout.property.TextAlignment
import java.io.File
import java.io.FileOutputStream

class DocumentFormatter(private val context: Context) {
    
    // Create formatted PDF
    fun createPDF(content: String, outputPath: String, title: String = ""): Boolean {
        return try {
            val pdfWriter = PdfWriter(outputPath)
            val pdfDoc = PdfDocument(pdfWriter)
            val document = Document(pdfDoc)
            
            // Add title
            if (title.isNotEmpty()) {
                val titlePara = Paragraph(title)
                    .setFontSize(20f)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                document.add(titlePara)
                document.add(Paragraph("\n"))
            }
            
            // Add content
            val paragraphs = content.split("\n\n")
            paragraphs.forEach { para ->
                if (para.trim().isNotEmpty()) {
                    document.add(Paragraph(para.trim()))
                }
            }
            
            document.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    // Create formatted Word document
    fun createWordDoc(content: String, outputPath: String, title: String = ""): Boolean {
        return try {
            val document = XWPFDocument()
            
            // Add title
            if (title.isNotEmpty()) {
                val titlePara = document.createParagraph()
                titlePara.alignment = ParagraphAlignment.CENTER
                val titleRun = titlePara.createRun()
                titleRun.setText(title)
                titleRun.isBold = true
                titleRun.fontSize = 20
            }
            
            // Add content
            val paragraphs = content.split("\n\n")
            paragraphs.forEach { para ->
                if (para.trim().isNotEmpty()) {
                    val p = document.createParagraph()
                    val run = p.createRun()
                    run.setText(para.trim())
                }
            }
            
            FileOutputStream(outputPath).use { out ->
                document.write(out)
            }
            document.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    // Format text with markdown-like syntax
    fun formatMarkdown(content: String, outputPath: String): Boolean {
        val formatted = StringBuilder()
        val lines = content.split("\n")
        
        lines.forEach { line ->
            when {
                line.startsWith("# ") -> formatted.append("<h1>${line.substring(2)}</h1>\n")
                line.startsWith("## ") -> formatted.append("<h2>${line.substring(3)}</h2>\n")
                line.startsWith("- ") -> formatted.append("<li>${line.substring(2)}</li>\n")
                line.startsWith("* ") -> formatted.append("<li>${line.substring(2)}</li>\n")
                line.trim().isEmpty() -> formatted.append("<br>\n")
                else -> formatted.append("<p>$line</p>\n")
            }
        }
        
        return try {
            File(outputPath).writeText(formatted.toString())
            true
        } catch (e: Exception) {
            false
        }
    }
    
    // Create resume/CV
    fun createResume(data: ResumeData, outputPath: String): Boolean {
        val content = buildString {
            append("${data.name}\n")
            append("${data.email} | ${data.phone}\n")
            append("${data.address}\n\n")
            
            append("SUMMARY\n")
            append("${data.summary}\n\n")
            
            append("EXPERIENCE\n")
            data.experience.forEach { exp ->
                append("${exp.title} at ${exp.company}\n")
                append("${exp.duration}\n")
                append("${exp.description}\n\n")
            }
            
            append("EDUCATION\n")
            data.education.forEach { edu ->
                append("${edu.degree} - ${edu.institution}\n")
                append("${edu.year}\n\n")
            }
            
            append("SKILLS\n")
            append(data.skills.joinToString(", "))
        }
        
        return createPDF(content, outputPath, data.name)
    }
    
    // Create invoice
    fun createInvoice(invoice: InvoiceData, outputPath: String): Boolean {
        val content = buildString {
            append("INVOICE\n\n")
            append("Invoice #: ${invoice.number}\n")
            append("Date: ${invoice.date}\n\n")
            
            append("Bill To:\n")
            append("${invoice.clientName}\n")
            append("${invoice.clientAddress}\n\n")
            
            append("ITEMS\n")
            invoice.items.forEach { item ->
                append("${item.description} - $${item.price} x ${item.quantity} = $${item.price * item.quantity}\n")
            }
            
            append("\nTotal: $${invoice.total}")
        }
        
        return createPDF(content, outputPath, "Invoice")
    }
    
    // Convert image text to formatted document
    fun imageToDocument(imagePath: String, outputPath: String, ocrProcessor: OCRProcessor): Boolean {
        var success = false
        ocrProcessor.extractTextFromImage(imagePath) { text ->
            success = createPDF(text, outputPath)
        }
        return success
    }
}

data class ResumeData(
    val name: String,
    val email: String,
    val phone: String,
    val address: String,
    val summary: String,
    val experience: List<Experience>,
    val education: List<Education>,
    val skills: List<String>
)

data class Experience(
    val title: String,
    val company: String,
    val duration: String,
    val description: String
)

data class Education(
    val degree: String,
    val institution: String,
    val year: String
)

data class InvoiceData(
    val number: String,
    val date: String,
    val clientName: String,
    val clientAddress: String,
    val items: List<InvoiceItem>,
    val total: Double
)

data class InvoiceItem(
    val description: String,
    val price: Double,
    val quantity: Int
)
