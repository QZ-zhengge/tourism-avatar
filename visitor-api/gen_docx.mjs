import fs from "fs";
import {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  Header, Footer, AlignmentType, BorderStyle, WidthType, ShadingType,
  PageNumber, HeadingLevel,
} from "docx";

const md = fs.readFileSync("z:/软件杯文档/数据存储架构文档.md", "utf-8");
const lines = md.split("\n");

// ── 简洁样式 ──
const TITLE_FONT = "微软雅黑";
const BODY_FONT = "等线";
const CODE_FONT = "Consolas";

const SIZE_T = 36;   // title 18pt
const SIZE_H1 = 28;  // h1 14pt
const SIZE_H2 = 26;  // h2 13pt
const SIZE_H3 = 24;  // h3 12pt
const SIZE_B = 22;   // body 11pt
const SIZE_C = 18;   // code 9pt

const LINE_SPACING = 360; // ~1.5x for body

function t(text, font, size, bold = false) {
  return new TextRun({ text, font, size, bold });
}

function p(children, opts = {}) {
  return new Paragraph({
    spacing: { line: LINE_SPACING },
    ...opts,
    children,
  });
}

// ── Parse markdown ──
const children = [];
let inCode = false, codeLines = [];
let tableHeaders = [], tableRows = [];
let tableColWidths = [];

function flushCodes() {
  for (const cl of codeLines) {
    children.push(p([t(cl || " ", CODE_FONT, SIZE_C)], {
      shading: { fill: "F2F2F2", type: ShadingType.CLEAR },
      spacing: { line: 280 },
    }));
  }
  codeLines = [];
}

function flushTable() {
  if (tableRows.length === 0) return;
  const colCount = tableHeaders.length || tableRows[0]?.length || 2;
  const colW = Math.floor(9026 / colCount);
  const b = { style: BorderStyle.SINGLE, size: 1, color: "999999" };
  const bd = { top: b, bottom: b, left: b, right: b };

  const allRows = [];
  if (tableHeaders.length > 0) {
    allRows.push(new TableRow({ children: tableHeaders.map(h =>
      new TableCell({
        borders: bd, width: { size: colW, type: WidthType.DXA },
        shading: { fill: "E8E8E8", type: ShadingType.CLEAR },
        margins: { top: 40, bottom: 40, left: 80, right: 80 },
        children: [p([t(h, BODY_FONT, SIZE_B, true)])],
      })
    )}));
  }
  for (const row of tableRows) {
    allRows.push(new TableRow({ children: row.map(c =>
      new TableCell({
        borders: bd, width: { size: colW, type: WidthType.DXA },
        margins: { top: 40, bottom: 40, left: 80, right: 80 },
        children: [p([t(c || "", BODY_FONT, SIZE_B)])],
      })
    )}));
  }
  children.push(new Table({
    width: { size: colW * colCount, type: WidthType.DXA },
    columnWidths: Array(colCount).fill(colW),
    rows: allRows,
  }));
  tableHeaders = []; tableRows = [];
}

for (let i = 0; i < lines.length; i++) {
  const raw = lines[i];
  const line = raw.trim();

  if (line === "---" && !inCode) continue;
  if (line.length === 0 && !inCode) { flushCodes(); flushTable(); continue; }

  if (line.startsWith("```")) {
    if (inCode) { flushCodes(); inCode = false; }
    else { flushTable(); inCode = true; }
    continue;
  }
  if (inCode) { codeLines.push(raw); continue; }

  const tc = line.replace(/\|/g, "").trim();
  if (raw.startsWith("|") && raw.endsWith("|")) {
    if (/^\|[-:\s|]+\|$/.test(line)) { continue; }
    const cells = raw.split("|").slice(1, -1).map(c => c.trim().replace(/\*\*(.+?)\*\*/g, "$1").replace(/`(.+?)`/g, "$1"));
    if (tableHeaders.length === 0 && i + 1 < lines.length && /^\|[-:\s|]+\|$/.test(lines[i + 1]?.trim() || "")) {
      tableHeaders = cells;
    } else tableRows.push(cells);
    continue;
  } else if (tableRows.length > 0) { flushTable(); }

  let text = line.replace(/\*\*(.+?)\*\*/g, "$1").replace(/`(.+?)`/g, "$1");
  if (!text) continue;

  if (/^#\s/.test(line) && !/^##/.test(line)) {
    children.push(p([t(text.replace(/^#\s+/, ""), TITLE_FONT, SIZE_T, true)], { alignment: AlignmentType.CENTER }));
  } else if (/^##\s/.test(line)) {
    children.push(p([t(text.replace(/^##\s+/, ""), BODY_FONT, SIZE_H1, true)], { spacing: { before: 240, after: 120 } }));
  } else if (/^###\s/.test(line)) {
    children.push(p([t(text.replace(/^###\s+/, ""), BODY_FONT, SIZE_H2, true)], { spacing: { before: 180, after: 100 } }));
  } else if (/^####\s/.test(line)) {
    children.push(p([t(text.replace(/^####\s+/, ""), BODY_FONT, SIZE_H3, true)]));
  } else if (/^[-*]\s/.test(line)) {
    children.push(p([t(text.replace(/^[-*]\s+/, ""), BODY_FONT, SIZE_B)], { indent: { left: 360 } }));
  } else if (/^\d+[\.\、]\s/.test(line)) {
    children.push(p([t(text, BODY_FONT, SIZE_B)], { indent: { left: 360 } }));
  } else {
    children.push(p([t(text, BODY_FONT, SIZE_B)]));
  }
}
flushTable(); flushCodes();

// ── Build doc ──
const doc = new Document({
  sections: [{
    properties: {
      page: {
        size: { width: 11906, height: 16838 },
        margin: { top: 1440, right: 1200, bottom: 1440, left: 1200 },
      },
    },
    headers: { default: new Header({ children: [] }) },
    footers: { default: new Footer({
      children: [new Paragraph({
        alignment: AlignmentType.CENTER,
        children: [new TextRun({ children: [PageNumber.CURRENT], font: BODY_FONT, size: SIZE_B })],
      })]
    })},
    children,
  }],
});

const buf = await Packer.toBuffer(doc);
fs.writeFileSync("z:/软件杯文档/数据存储架构文档.docx", buf);
console.log("OK → z:/软件杯文档/数据存储架构文档.docx");
