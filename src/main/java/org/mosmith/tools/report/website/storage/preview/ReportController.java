/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mosmith.tools.report.website.storage.preview;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mosmith.tools.report.engine.execute.context.ExecuteContext;
import org.mosmith.tools.report.engine.execute.script.javascript.JSContextAware;
import org.mosmith.tools.report.engine.execute.script.javascript.JSEngineTopLevel;
import org.mosmith.tools.report.engine.output.ReportHelper;
import org.mosmith.tools.report.engine.util.StringUtils;
import org.mosmith.tools.report.website.storage.sharescript.ShareScriptManagerService;
import org.mosmith.tools.report.website.storage.template.TemplateManagerService;
import org.mosmith.tools.report.website.storage.utils.IOUtils;
import org.mozilla.javascript.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author Administrator
 */
@Controller
@RequestMapping("/report")
public class ReportController {
    
    private static final String DATA="data";
    private static final String CODE="code";
    
    @Autowired
    TemplateManagerService templateManagerService;
    
    @Autowired
    ShareScriptManagerService shareScriptManagerService;
    
    @PostMapping("/preview")
    public void preview(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
        String templateData=httpServletRequest.getParameter("templateData");
        String previewDataJson=httpServletRequest.getParameter("previewData");
        String previewOptionsJson=httpServletRequest.getParameter("previewOptions");
        
        String templateId=httpServletRequest.getParameter("templateId");
        if(templateId!=null && !templateId.trim().isEmpty()) {
            Map<String, Object> templateInfo=templateManagerService.getTemplate(templateId);
            templateData=new String((byte[]) templateInfo.get(DATA), "utf-8");
        }
        
        Reader templateDataReader=new StringReader(templateData);
        Object previewData=readJson(previewDataJson);
        Map<String, Object> previewOptions=(Map<String, Object>) readJson(previewOptionsJson);

        String docType=StringUtils.nonull(previewOptions.get("docType"));
        if (httpServletRequest.getParameter("docType")!=null) {
            docType=httpServletRequest.getParameter("docType");
        }
        
        File file;
        String fileName;
        String contentType;
        if(docType.equalsIgnoreCase("PDF")) {
            file=getReportHelper().toPdf(templateDataReader, previewData, previewOptions);
            fileName="preview.pdf";
            contentType="application/pdf";
        } else if(docType.equalsIgnoreCase("Word")) {
            file=getReportHelper().toWord(templateDataReader, previewData, previewOptions);
            fileName="preview.docx";
            contentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if(docType.equalsIgnoreCase("Excel")) {
            file=getReportHelper().toExcel(templateDataReader, previewData, previewOptions);
            fileName="preview.xlsx";
            contentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else if(docType.equalsIgnoreCase("HTML")) {
            file=getReportHelper().toHtml(templateDataReader, previewData, previewOptions);
            fileName="preview.html";
            contentType="text/html";
        } else if(docType.equalsIgnoreCase("Image")) {
            // previewOptions.put("imgDpi", 300);
            // previewOptions.put("imgFormat", "png");
            List<File> imageFiles=getReportHelper().toImages(templateDataReader, previewData, previewOptions);
            if(imageFiles.isEmpty()) {
                imageFiles.add(File.createTempFile("output-", ".png"));
            }
            for(int i=1;i<imageFiles.size();i++) {
                imageFiles.get(i).delete();
            }
            file=imageFiles.get(0);
            fileName="preview.png";
            contentType="image/png";
        } else {
            file=getReportHelper().toPdf(templateDataReader, previewData, previewOptions);
            fileName="preview.pdf";
            contentType="application/pdf";
        }
        
        InputStream fileIs=null;
        try {
            httpServletResponse.setContentType(contentType);
            httpServletResponse.setHeader("Content-Disposition", "inline;filename=" + fileName);
            
            fileIs=new FileInputStream(file);
            OutputStream os=httpServletResponse.getOutputStream();
            IOUtils.copyStream(fileIs, os);
        } finally {
            IOUtils.close(fileIs);
            if(file!=null) {
                boolean deleted=file.delete();
            }
        }
    }
    
    private ReportHelper getReportHelper() {
        ReportHelper reportHelper=new ReportHelper();
        reportHelper.setBuiltIn("scriptLoader", new ScriptLoader());
        reportHelper.setBuiltIn("jdbcExecutor", new JDBCExecutor());
        return reportHelper;
    }
    
    private Object readJson(String json) throws IOException {
        if(json==null || json.isEmpty()) {
            return new HashMap();
        }

        Character firstC=null;
        for(int i=0, length=json.length();i<length;i++) {
            char c= json.charAt(i);
            if(Character.isWhitespace(c)) {
                continue;
            }
            
            firstC=c;
            break;
        }
        
        if(firstC=='[') {
            ObjectMapper objectMapper=new ObjectMapper();
            
            JavaType listType=objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class);
            List<Object> list=objectMapper.readValue(json, listType);
            return list;
        } else if(firstC=='{') {
            ObjectMapper objectMapper=new ObjectMapper();
            
            Map map=objectMapper.readValue(json, Map.class);
            return map;
        } else {
            throw new IllegalArgumentException("Invalid json data!");
        }
    }

    public class ScriptLoader implements JSContextAware {
        private Context context;
        private JSEngineTopLevel topLevel;
        
        @Override
        public void setContext(Context context) {
            this.context=context;
        }

        @Override
        public void setTopLevel(JSEngineTopLevel topLevel) {
            this.topLevel=topLevel;
        }
        
        public Object load(String scriptPath) {
            Map<String, Object> scriptInfo=shareScriptManagerService.getScriptByPath(scriptPath);
            if(scriptInfo==null) {
                return null;
            }
            
            String code=(String) scriptInfo.get(CODE);
            if(code==null) {
                return null;
            }
            
            Object jsObject=context.evaluateString(topLevel, code, scriptPath, 1, null);
            return jsObject;
        }
    }
    
    public class JDBCExecutor {
        private final Pattern placeHolderPattern=Pattern.compile("\\$\\{(.*?)\\}");
        
        public Object executeJDBCQuery(Map<String, Object> model) throws Exception {
            String driverClass=nonull(model.get("driver"));
            String jdbcUrl=nonull(model.get("jdbcUrl"));
            String user=nonull(model.get("user"));
            String password=nonull(model.get("password"));
            String sql=nonull(model.get("sql"));
            
            sql=replacePlaceHolders(sql);
            
            Class.forName(driverClass);
            Connection connection=null;
            Statement statement=null;
            ResultSet resultSet=null;
            try {
                connection=DriverManager.getConnection(jdbcUrl, user, password);
                statement=connection.createStatement();
                resultSet=statement.executeQuery(sql);

                ResultSetMetaData metaData=resultSet.getMetaData();
                int colCount=metaData.getColumnCount();
                String[] colLabels=new String[colCount];
                for(int i=1;i<=colCount;i++) {
                    colLabels[i-1]=metaData.getColumnLabel(i);
                }
                
                List<Object> result=new ArrayList<Object>();
                while(resultSet.next()) {
                    Map<String, Object> rowData=new LinkedHashMap<String, Object>();
                    for(String colLabel: colLabels) {
                        rowData.put(colLabel, resultSet.getObject(colLabel));
                    }
                    result.add(rowData);
                }
                return result;
            } finally {
                close(resultSet);
                close(statement);
                close(connection);
            }
        }

        private void close(Object object) {
            try {
                if(object instanceof Connection) {
                    ((Connection)object).close();
                }
                if(object instanceof Statement) {
                    ((Statement)object).close();
                }
                if(object instanceof ResultSet) {
                    ((ResultSet)object).close();
                }
                if(object instanceof Closeable) {
                    ((Closeable)object).close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        private String replacePlaceHolders(String content) {
            
            StringBuilder sb=new StringBuilder();
            int start=0;
            int end=0;
            
            while(true) {
                Matcher matcher=placeHolderPattern.matcher(content);
                if(!matcher.find()) {
                    break;
                }
                while(true) {
                    start=matcher.start();
                    sb.append(content.substring(end, start));
                    end=matcher.end();
                    
                    matcher.toMatchResult();
                    String expression=matcher.group(1);
                    
                    Object value=ExecuteContext.get().lookup(expression);
                    if(value!=ExecuteContext.NOT_FOUND && value!=null) {
                        sb.append(value);
                    }
                    
                    if(!matcher.find()) {
                        break;
                    }
                }
                
                sb.append(content.substring(end));
                
                content=sb.toString();
                sb=new StringBuilder();
            }
            
            return content;
        }
    }

    private static String nonull(Object value) {
        if(value==null) {
            return "";
        }
        
        String valueString= value.toString();
        if(valueString==null) {
            valueString="";
        }
        return valueString;
    }
    
}

