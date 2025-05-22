package graph.WAL;

import graph.exceptions.InvalidLogOperationException;

import java.util.*;
import java.util.zip.CRC32;

import static graph.WAL.LoggingInfo.LoggingInfoBuilder.aLoggingInfo;
import static graph.WAL.LoggingInfo.LoggingInfoBuilder;

public class WALParser {

    public List<LoggingInfo> parseTransaction(List<String> transaction) throws InvalidLogOperationException {
        List<LoggingInfo> loggingInfos = new LinkedList<>();
        for (String operation : transaction) {
            loggingInfos.add(parse(operation));
        }
        return loggingInfos;
    }

    private LoggingInfo parse(String line) throws InvalidLogOperationException {
        if (!line.contains("|")) {
            throw new InvalidLogOperationException(line);
        }

        String[] separated = line.split("\\|");
        String operationDetails = separated[0];
        String checkSum = separated[1].trim();
        checkSumCheck(checkSum, operationDetails);   // Checks if this current log is valid or not

        String operationType = operationDetails.substring(0, operationDetails.indexOf(" "));
        String operationArgs = operationDetails.substring(operationDetails.indexOf(" ") + 1).trim();
        LoggingOperations operation = LoggingOperations.valueOf(operationType);
        return parseArguments(operation, operationArgs);
    }

    private void checkSumCheck(String checkSum, String operation) throws InvalidLogOperationException {
        CRC32 crc32 = new CRC32();
        crc32.update(operation.trim().getBytes());
        if (!checkSum.equals(Long.toHexString(crc32.getValue()))) {
            throw new InvalidLogOperationException(operation);
        }
    }

    private LoggingInfo parseArguments(LoggingOperations operation, String operationArgs) {
        String[] args = operationArgs.split("~");
        LoggingInfoBuilder loggingInfoBuilder = aLoggingInfo(operation);

        if (operation.equals(LoggingOperations.COMMIT)) {
            return loggingInfoBuilder.build();
        }

        for (String arg : args) {
            arg = arg.trim();
            String key = arg.substring(0, arg.indexOf("="));
            String value = arg.substring(arg.indexOf("=") + 1).trim();
            loggingInfoBuilder = switch (key) {
                case "id" -> loggingInfoBuilder.withId(value);
                case "source" -> loggingInfoBuilder.withSource(value);
                case "target" -> loggingInfoBuilder.withTarget(value);
                case "weight" -> loggingInfoBuilder.withWeight(Double.parseDouble(value));
                case "key" -> loggingInfoBuilder.withKey(value);
                case "value" ->
                        loggingInfoBuilder.withValue(parseObject(value));
                case "attributes", "properties"
                        -> loggingInfoBuilder.withAttributes((Map<String, Object>) parseObject(value));
                default -> loggingInfoBuilder;
            };
        }
        return loggingInfoBuilder.build();
    }

    private Map<String, Object> parseMap(String mapping) {
        String[] pairs = mapping.split(",");
        Map<String, Object> result = new HashMap<>();
        for (String pair : pairs) {
            if (pair.contains("=")) {
                String[] keyValue = pair.split("=");
                result.put(keyValue[0].trim(), parseObject(keyValue[1].trim()));
            }
        }
        return result;
    }

    private List<Object> parseList(String list) {
        String[] itemsList = list.split(",");
        List<Object> items = new LinkedList<>();
        for (String item : itemsList) {
            items.add(parseObject(item.trim()));
        }
        return items;
    }

    private Object parseObject(String value) {
        // Map
        if (value.startsWith("{") && value.endsWith("}")) {
            return parseMap(value.substring(1, value.length() - 1));
        }

        // List
        if (value.startsWith("[") && value.endsWith("]")) {
            return parseList(value.substring(1, value.length() - 1));
        }

        // Boolean
        if (Objects.equals(value, "true") | Objects.equals(value, "false")) {
            return Boolean.parseBoolean(value);
        }

        // Integer
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {}

        // Double
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {}

        // String
        return value;
    }
}
