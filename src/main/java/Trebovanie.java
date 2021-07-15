import com.jayway.jsonpath.JsonPath;
import okhttp3.*;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Trebovanie {
    public static void main(String[] args) throws IOException {


        String nd, knds, guid, processid;

        if (args.length == 3) {
            processid = args[0];
            nd = args[1];
            knds = args[2];
        }
        else {
            Scanner in = new Scanner(System.in);
            System.out.println("Введите processId");
            processid = in.nextLine();
            System.out.println("Введите наименование НД.");
            nd = in.nextLine();
            System.out.println("Введите knds (одно или через запятую). Кавычки обязательны. Пример: \"1165050\",\"1165050\"");
            knds = in.nextLine();
        }

        //nd = "NO_NDS_0087_0087_7703376553770301001_20200510_5ed17326-911c-49ab-9908-ce9d37817ea8";
        //knds = "\"1165013\"";
        //processid = "586a8e10-b710-11eb-88e7-1e298ea96bb1";


        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(5, TimeUnit.MINUTES).writeTimeout(5, TimeUnit.MINUTES).readTimeout(5, TimeUnit.MINUTES);
        OkHttpClient client = builder.build();
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");


        RequestBody body = RequestBody.create(mediaType, "client_id=rec_elk_m2m&client_secret=password&realm=customer&grant_type=urn:roox:params:oauth:grant-type:m2m&service=dispatcher");
        Request request = new Request.Builder()
                .url("http://uidm.uidm-dev.d.exportcenter.ru/sso/oauth2/access_token")
                .method("POST", body)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Cookie", "RX_SID=3DF100A3865918B488C793E02129E6D1; lb=expc")
                .build();
        Response response = client.newCall(request).execute();
        String jsonString = response.body().string();
        String execution = JsonPath.read(jsonString, "$.execution");
        //pre-token

        body = RequestBody.create(mediaType, "client_id=rec_elk_m2m&client_secret=password&realm=/customer&grant_type=urn:roox:params:oauth:grant-type:m2m&service=dispatcher&_eventId=next&username=mdm_admin&password=password&execution=" + execution);
        request = new Request.Builder()
                .url("http://uidm.uidm-dev.d.exportcenter.ru/sso/oauth2/access_token")
                .method("POST", body)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Cookie", "RX_SID=F45C0783BC9DD13A5E57DDA3D6E7F31B; lb=expc")
                .build();

        response = client.newCall(request).execute();
        jsonString = response.body().string();
        String token = "Bearer sso_1.0_" + JsonPath.read(jsonString, "$.access_token");
        //token

        request = new Request.Builder()
                .url("http://zvat-kontur-adapter-korus.integration-dev.d.exportcenter.ru/kontur-adapter/api/v1/profiles/4a27ff58-7ee0-488f-9cd1-6c7c5035513e/authsid")
                .method("GET", null)
                .addHeader("accept", "*/*")
                .addHeader("Authorization", token)
                .build();

        response = client.newCall(request).execute();
        jsonString = response.body().string();
        String sid = "auth.sid " + JsonPath.read(jsonString, "$.sid");
        //sid

        request = new Request.Builder()
                .url("http://bpmn-api-service.bpms-dev.d.exportcenter.ru/bpmn/api/v1/bpmn/tasks?processInstanceId="+processid)
                .method("GET", null)
                .addHeader("accept", "*/*")
                .addHeader("camundaId", "camunda-mdm")
                .addHeader("Authorization", token)
                .build();
        response = client.newCall(request).execute();
        jsonString = response.body().string();
        int index = jsonString.lastIndexOf("Заглушка");
        if (index == -1){
            System.out.println("Заглушка отсутствует");
        }
        else{
            guid = jsonString.substring(index-46,index-10);
            mediaType = MediaType.parse("application/json");
            body = RequestBody.create(mediaType, "{ \"variables\": { \"additionalProp1\": { \"type\": \"string\", \"value\": {}, \"valueInfo\": { \"objectTypeName\": \"string\", \"serializationDataFormat\": \"string\" } }, \"additionalProp2\": { \"type\": \"string\", \"value\": {}, \"valueInfo\": { \"objectTypeName\": \"string\", \"serializationDataFormat\": \"string\" } }, \"additionalProp3\": { \"type\": \"string\", \"value\": {}, \"valueInfo\": { \"objectTypeName\": \"string\", \"serializationDataFormat\": \"string\" } } }}");
            request = new Request.Builder()
                    .url("http://bpmn-api-service.bpms-dev.d.exportcenter.ru/bpmn/api/v1/bpmn/tasks/"+guid+"/finish")
                    .method("POST", body)
                    .addHeader("accept", "*/*")
                    .addHeader("camundaId", "camunda-mdm")
                    .addHeader("Authorization", token)
                    .addHeader("Content-Type", "application/json")
                    .build();
            response = client.newCall(request).execute();
            System.out.println("Финишируем заглушку. Статус: "+response.code());
            if (response.code()!=200){
                System.out.println(jsonString);
            }
        }
        //поиск таски

        mediaType = MediaType.parse("application/json-patch+json");
        String bodyString = "{\"accountId\": \"551f0bf2-2336-41dc-996f-6236890f1754\",\"sender\": {\"inn\": \"7703376553\",\"kpp\": \"770301001\",\"name\": \"АО РОССИЙСКИЙ ЭКСПОРТНЫЙ ЦЕНТР\",\"certificate\": {\"content\": \"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\"}},\"payer\": {\"inn\": \"7703376553\",\"name\": \"АО РОССИЙСКИЙ ЭКСПОРТНЫЙ ЦЕНТР\",\"organization\": {\"kpp\": \"770301001\"},\"chiefFio\": {\"surname\": \"Богданов\",\"name\": \"Иван\",\"patronymic\": \"Иосифович\"}}," +
                "\"knds\": ["+ knds +"],\"ifnsCode\": \"0087\", \"sentOnReportFilename\": " +
                "\"" +
                nd +"\"}";

        body = RequestBody.create(mediaType, bodyString);

        request = new Request.Builder()
                .url("https://extern-api.testkontur.ru/test-tools/v1/generate-demand")
                .method("POST", body)
                .addHeader("accept", "text/plain")
                .addHeader("Authorization", sid)
                .addHeader("X-Kontur-Apikey", "706d51e7-5309-4a38-a130-7b71987647d9")
                .addHeader("Content-Type", "application/json-patch+json")
                .build();

        response = client.newCall(request).execute();
        jsonString = response.body().string();
        System.out.println("Статус генерации требования: "+response.code());
        if (response.code()!=200){
            System.out.println(jsonString);
        }

        System.exit(0);
    }
}
