package de.hpi.isg.dao;

import de.hpi.isg.elements.AnnotationResults;
import de.hpi.isg.elements.Sheet;
import lombok.Getter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
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
        Sheet sheet = annotationResults.getSheet();
        String fileName = sheet.getFileName();
        String spreadSheetName = sheet.getSheetName();
        String query = String.format("insert into data_file (excel_file_name, spread_sheet_name) values (\'%s\', \'%s\')", fileName, spreadSheetName);

        executeUpdate(query);

        final int data_file_id = getDataFileIdByDataFileNameAndSpreadsheetName(fileName, spreadSheetName);

        List<AnnotationResults.AnnotationResult> results = annotationResults.getAnnotationResults();
        results.forEach(result -> {
            int startLineNumber = result.getStartLineNumber();
            int endLineNumber = result.getEndLineNumber();
            AnnotationResults.LineType lineType = result.getType();
            executeUpdate(String.format("insert into line_function (data_file_id, start_line_number, end_line_number, line_type) values (%d, %d, %d, \'%s\')",
                    data_file_id, startLineNumber, endLineNumber, lineType.toString()));
        });

        System.out.println("Records created successfully");
    }

    @Override
    public int getDataFileIdByDataFileNameAndSpreadsheetName(String dataFileName, String spreadSheetName) {
        String query = String.format("select id from data_file where excel_file_name = \'%s\' and spread_sheet_name = \'%s\'", dataFileName, spreadSheetName);
        Connection connection = databaseConnector.getConnection();
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

    private void executeUpdate(String query) {
        Connection connection = databaseConnector.getConnection();

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
