package FlowSlicer.XMLObject;

import lombok.extern.slf4j.Slf4j;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
@Slf4j
public class XMLHelper {
    public static void parseFlowDroidXmlFile(String xmlPath, FlowDroidResultModel model){
        List<Reference> fromReferenceList = new ArrayList<>();
        List<Reference> toReferenceList = new ArrayList<>();
        Map<Reference, List<Reference>> sinkToSourcesMap = new HashMap<>();
        List<Leak> leaks = null;

        try {
            File xmlFile = new File(xmlPath);
            JAXBContext jaxbContext = JAXBContext.newInstance(DataFlowResults.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            DataFlowResults dataFlowResults = (DataFlowResults)jaxbUnmarshaller.unmarshal(xmlFile);

            leaks = dataFlowResults.getLeaks().getLeaks();
            model.setLeakNum(leaks.size());
            for (Leak leak : leaks) {
                Sink sink = leak.getSink();
                String[] sinkMethodSplit = sink.getMethod().split(": ");
                String sinkStatement = sink.getStatement();

                // 匹配方法体
                String methodPattern = "<(.*?)>";
                Pattern methodRegex = Pattern.compile(methodPattern);
                Matcher methodMatcher = methodRegex.matcher(sinkStatement);
                String methodBody = null;
                if (methodMatcher.find()) {
                     methodBody = methodMatcher.group(0);
                } else {
                    log.error(sink.toString() + "Method Body not found.");
                }

                // 匹配参数
                String paramsPattern = "\\(([^()]*)\\)$";
                Pattern paramsRegex = Pattern.compile(paramsPattern);
                Matcher paramsMatcher = paramsRegex.matcher(sinkStatement);

                boolean isAssignment = sinkStatement.contains("=");
                String statementLeftBox = null;
                String statementInvoker = null;
                String invokeType = null;

                if (isAssignment) {
                    // 分离赋值符号左边部分
                    statementLeftBox = sinkStatement.split("=")[0].trim();
                    // 分离赋值符号右边部分并提取invokeType和statementInvoker
                    String rightSide = sinkStatement.split("=")[1].trim();
                    invokeType = rightSide.split(" ")[0].trim();
                    statementInvoker = rightSide.split(" ")[1].split("\\.")[0].trim();
                } else {
                    // 提取invokeType和statementInvoker
                    invokeType = sinkStatement.split(" ")[0].trim();
                    statementInvoker = sinkStatement.split(" ")[1].split("\\.")[0].trim();
                }

                Reference targetTo = new Reference("to",
                        sinkMethodSplit[0].substring(1),
                        sinkMethodSplit[1].substring(0, sinkMethodSplit[1].length() - 1),
                        isAssignment,
                        statementLeftBox,
                        statementInvoker,
                        invokeType,
                        methodBody,
                        matchParams(paramsMatcher, sink.toString())
                );
                toReferenceList.add(targetTo);

                Sources sources = leak.getSources();
                List<Reference> sourceReferenceList = new ArrayList<>();
                for (Source source : sources.getSources()) {
                    String[] sourceMethodSplit = source.getMethod().split(": ");
                    String sourceStatement = source.getStatement();

                    // 匹配方法体
                    methodMatcher = methodRegex.matcher(sourceStatement);
                    if (methodMatcher.find()) {
                        methodBody = methodMatcher.group(0);
                    } else {
                        log.error(source.toString() + "Method Body not found.");
                    }

                    // 匹配参数
                    paramsMatcher = paramsRegex.matcher(sourceStatement);
                    isAssignment = sourceStatement.contains("=");
                    statementLeftBox = null;
                    statementInvoker = null;
                    invokeType = null;

                    if (!isAssignment) {
                        // 提取invokeType和statementInvoker
                        invokeType = sourceStatement.split(" ")[0].trim();
                        statementInvoker = sourceStatement.split(" ")[1].split("\\.")[0].trim();
                    } else {
                        // 分离赋值符号左边部分
                        statementLeftBox = sourceStatement.split("=")[0].trim();
                        // 分离赋值符号右边部分并提取invokeType和statementInvoker
                        String rightSide = sourceStatement.split("=")[1].trim();
                        invokeType = rightSide.split(" ")[0].trim();
                        statementInvoker = rightSide.split(" ")[1].split("\\.")[0].trim();
                    }

                    Reference targetFrom = new Reference("from",
                            sourceMethodSplit[0].substring(1),
                            sourceMethodSplit[1].substring(0, sourceMethodSplit[1].length() - 1),
                            isAssignment,
                            statementLeftBox,
                            statementInvoker,
                            invokeType,
                            methodBody,
                            matchParams(paramsMatcher, source.toString())
                    );
                    sourceReferenceList.add(targetFrom);
                    fromReferenceList.add(targetFrom);
                }
                sinkToSourcesMap.put(targetTo, sourceReferenceList);
            }
        } catch (Exception e) {
            log.error("Error reading input slicing criteria.");
        }

        model.setSinkList(toReferenceList);
        model.setSourceList(fromReferenceList);
        model.setSinkToSourcesMap(sinkToSourcesMap);
    }

    private static List<String> matchParams(Matcher paramsMatcher, String string) {
        List<String> paramList = new ArrayList<>();
        if (paramsMatcher.find()) {
            String params = paramsMatcher.group(1);
            Pattern paramPattern = Pattern.compile("\"[^\"]*\"|[^,]+");
            Matcher paramMatcher = paramPattern.matcher(params);
            while (paramMatcher.find()) {
                paramList.add(paramMatcher.group().trim());
            }
        } else {
            log.error(string + "Parameters not found.");
        }
        return paramList;
    }
}
