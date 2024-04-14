package zone.yukai.rapidDataTransfer;

import zone.yukai.rapidDataTransfer.common.Column;
import zone.yukai.rapidDataTransfer.common.ConfigItem;
import zone.yukai.rapidDataTransfer.common.IWriter;
import zone.yukai.rapidDataTransfer.common.Row;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

public class PostgresWriter implements IWriter {
    String postgresUrl;
    String postgresUsername;
    String postgresPassword;
    String tableName;
    @Override
    public void init(Map<String, Object> setting) {
        Set<String> strings = setting.keySet();
        List<String> configList = new ArrayList<>(strings);
        postgresUrl = (String) ((Map<String, Object>) setting.get(configList.get(1))).get("url");
        postgresUsername = (String) ((Map<String, Object>) setting.get(configList.get(1))).get("username");
        postgresPassword = (String) ((Map<String, Object>) setting.get(configList.get(1))).get("password");
        tableName = (String) ((Map<String, Object>) setting.get(configList.get(1))).get("tableName");
    }

    @Override
    public void write(LinkedBlockingQueue<Row> channel) {
        while (true){
            try {
                Row row = channel.take();
                System.out.println("收到一行写入任务");
                List<Column> columnList = row.getRowList();
                StringBuilder columnName = new StringBuilder();
                StringBuilder columnValue = new StringBuilder();
                for(int i = 0;i < columnList.size();i++){
                    columnName.append(columnList.get(i).getName());

                    Object value = columnList.get(i).getValue();

                    String dataType = columnList.get(i).getDataType();
                    switch (dataType) {
                        case "BIGINT":
                            columnValue.append(value);
                            break;
                        case "VARCHAR":
                            columnValue.append("\""+value+"\"");
                            break;
                        case "varchar":
                            columnValue.append("\""+value+"\"");
                            break;
                        case "INT":
                            columnValue.append(value);
                            break;
                        case "TIMESTAMP":
                            columnValue.append("\""+value+"\"");
                            break;
                        default:
                            columnValue.append(columnList.get(i).getValue());
                    }
                    if(i < columnList.size()-1){
                        columnName.append(',');
                        columnValue.append(',');
                    }
                }
                String sql = "INSERT INTO " + tableName + "("+columnName.toString()+")values("+columnValue.toString()+");";
                Connection connection = DriverManager.getConnection(postgresUrl, postgresUsername, postgresPassword);
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                preparedStatement.execute();

                connection.close();
                preparedStatement.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public List<ConfigItem> getConfigItems() {
        List<ConfigItem> items = new ArrayList<>();
        items.add(new ConfigItem("POSTGRES.URL","string","url"));
        items.add(new ConfigItem("POSTGRES.USERNAME","string","用户名"));
        items.add(new ConfigItem("POSTGRES.PASSWORD","string","密码"));
        items.add(new ConfigItem("POSTGRES.TABLE_NAME","string","表名"));
        return items;
    }
}
