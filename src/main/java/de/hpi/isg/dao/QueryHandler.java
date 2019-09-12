package de.hpi.isg.dao;

import de.hpi.isg.elements.AnnotationResults;
import de.hpi.isg.elements.Sheet;
import lombok.Getter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This class implements the queries specified by the {@link AbstractQueries} in relational DBMS environment.
 *
 * @author Lan Jiang
 * @since 9/10/19
 */
public class QueryHandler implements AbstractQueries {

    @Getter
    private final DatabaseConnector databaseConnector;

    public QueryHandler() {
        this.databaseConnector = new DatabaseConnector();
    }

    @Override
    public void insertLineFunctionAnnotationResults(AnnotationResults annotationResults) {
        Connection connection = databaseConnector.getConnection();

        Sheet sheet = annotationResults.getSheet();
        String fileName = sheet.getFileName().replace("'", "''");
        String spreadSheetName = sheet.getSheetName();

        final int spreadsheet_id = getSpreadsheetIdByName(spreadSheetName, fileName, connection);


        int lastEndLineNumber = 0;
        List<AnnotationResults.AnnotationResult> results = annotationResults.getAnnotationResults();
        for (AnnotationResults.AnnotationResult result : results) {
            int startLineNumber = result.getStartLineNumber();
            int endLineNumber = result.getEndLineNumber();
            AnnotationResults.LineType lineType = result.getType();

            // fill the blank row block automatically.
            if (startLineNumber > lastEndLineNumber + 1) {
                String query = String.format("insert into line_function (spreadsheet_id, start_line_number, end_line_number, line_type) values (%d, %d, %d, \'%s\')",
                        spreadsheet_id, lastEndLineNumber + 1, startLineNumber - 1, AnnotationResults.LineType.EMPTY);
                executeUpdate(query, databaseConnector.getConnection());
            }

            executeUpdate(String.format("insert into line_function (spreadsheet_id, start_line_number, end_line_number, line_type) values (%d, %d, %d, \'%s\')",
                    spreadsheet_id, startLineNumber, endLineNumber, lineType.toString()), databaseConnector.getConnection());
            lastEndLineNumber = endLineNumber;
        }
//        System.out.println("Records created successfully");
    }

    @Override
    public int getDataFileIdByDataFileNameAndSpreadsheetName(String dataFileName, String spreadSheetName) {
        String query = String.format("select id from spreadsheet where excel_file_name = \'%s\' and spread_sheet_name = \'%s\'", dataFileName, spreadSheetName);
        return getId(query, null);
    }

    @Override
    public Sheet getMostSimilarSheet(int spreadsheet_id) {
        String query = String.format("select * from spreadsheet_similarity, spreadsheet " +
                "where spreadsheet.id = spreadsheet_similarity.spreadsheet_id_1 and spreadsheet_similarity.spreadsheet_id_1 = %d " +
                "order by similarity descending", spreadsheet_id);

        return null;
    }

    @Override
    public int getExcelFileIdByName(String excelFileName, Connection connection) {
        String query = String.format("select id from excel_file where excel_file_name = \'%s\'", excelFileName);
        return getId(query, connection);
    }

    @Override
    public int getSpreadsheetIdByName(String spreadsheetName, String excel_file_name, Connection connection) {
        String query = String.format("select spreadsheet.id from spreadsheet, excel_file where excel_file.excel_file_name = '%s' " +
                "and excel_file.id = spreadsheet.excel_file_id and" +
                " spreadsheet.spread_sheet_name = \'%s\'", excel_file_name, spreadsheetName);
        return getId(query, connection);
    }

    private int getId(String query, Connection connection) {
        ResultSet resultSet;

        int id = 0;
        try {
            Statement statement = connection.createStatement();
            resultSet = statement.executeQuery(query);
            if (resultSet.next()) {
                id = resultSet.getInt("id");
            }

            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return id;
    }

    @Override
    public void loadExcelFileStatistics(Map<String, List<String>> sheetNamesByExcelFileName) {
        Connection connection;
        try {
            connection = databaseConnector.getConnection();

            for (Map.Entry<String, List<String>> entry : sheetNamesByExcelFileName.entrySet()) {
                String excelFileName = entry.getKey().replace("'", "''");
                int spreadSheetAmount = entry.getValue().size();
                executeUpdate(String.format("insert into excel_file (excel_file_name, spreadsheet_number) values (\'%s\', %d)",
                        excelFileName, spreadSheetAmount), connection);
//                try {
//                    statement.addBatch(String.format("insert into excel_file (excel_file_name, spreadsheet_number) values (\'%s\', %d)",
//                            excelFileName, spreadSheetAmount));
//                } catch (SQLException e) {
//                    e.printStackTrace();
//                }

                final int excel_file_id = getExcelFileIdByName(excelFileName, connection);
//                System.out.println(excel_file_id);

                for (String spreadSheetName : entry.getValue()) {
                    String query = String.format("insert into spreadsheet (excel_file_id, spread_sheet_name) values (%d, \'%s\')", excel_file_id, spreadSheetName.replace("'", "''"));
                    executeUpdate(query, connection);
//                    try {
//                        statement.addBatch(query);
//                    } catch (SQLException e) {
//                        e.printStackTrace();
//                    }
                }
            }
//            connection.commit();

            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateSpreadsheetAnnotationStatus(String spreadsheetName, String excel_file_name) {
        Connection connection = databaseConnector.getConnection();

        int spreadsheet_id = getSpreadsheetIdByName(spreadsheetName, excel_file_name, connection);
        String query = String.format("update spreadsheet set has_annotated = TRUE where id = %d", spreadsheet_id);
        executeUpdate(query, connection);

        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Sheet> getAllUnannotatedSpreadsheet() {
        Connection connection = databaseConnector.getConnection();

        String query = "select spreadsheet.spread_sheet_name, excel_file.excel_file_name, excel_file.spreadsheet_number from spreadsheet, excel_file " +
                "where spreadsheet.excel_file_id = excel_file.id and spreadsheet.has_annotated = FALSE";

        List<Sheet> results = new LinkedList<>();
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(query);

            while (resultSet.next()) {
                String spreadsheetName = resultSet.getString("spread_sheet_name");
                String excelFileName = resultSet.getString("excel_file_name");
                int spreadSheetAmount = resultSet.getInt("spreadsheet_number");
                Sheet sheet = new Sheet(spreadsheetName, excelFileName, spreadSheetAmount);
                results.add(sheet);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    @Override
    public Sheet getSheetById(int id) {
        Connection connection = databaseConnector.getConnection();

        String query = String.format("select spreadsheet.spread_sheet_name, excel_file.excel_file_name, excel_file.spreadsheet_number from spreadsheet, excel_file " +
                "where spreadsheet.excel_file_id = excel_file.id and spreadsheet.id = %d", id);
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(query);

            Sheet sheet = null;
            if (resultSet.next()) {
                String sheetName = resultSet.getString("spread_sheet_name");
                String excelName = resultSet.getString("excel_file_name");
                int sheetAmount = resultSet.getInt("spreadsheet_number");
                sheet = new Sheet(sheetName, excelName, sheetAmount);
            }
            return sheet;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public int getSheetAmountByExcelName(String excelName) {
        Connection connection = databaseConnector.getConnection();

        String query = String.format("select spreadsheet_number from excel_file where excel_file_name = '%s'", excelName);

        int sheetAmount = 0;
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(query);

            if (resultSet.next()) {
                sheetAmount = resultSet.getInt("spreadsheet_number");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sheetAmount;
    }

    private void executeUpdate(String query, Connection connection) {
        try {
            Statement statement = connection.createStatement();
            statement.executeUpdate(query);

            statement.close();
            connection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
