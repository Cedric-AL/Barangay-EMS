import barangayems.backend.*;
import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

public final class RequirementsTransportChecks {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        var route=new AtomicReference<String>();var body=new AtomicReference<JsonObject>();
        var snapshot=new AtomicReference<String>("{\"centers\":[],\"people\":[],\"supplies\":[],\"allocations\":[],\"transfers\":[],\"supplyRules\":[]}");
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/",exchange->{
            route.set(exchange.getRequestURI().getPath());String request=new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);
            if(!request.isBlank())body.set(JsonParser.parseString(request).getAsJsonObject());
            String response=route.get().endsWith("ems_snapshot")?snapshot.get():"null";
            if(route.get().endsWith("/token"))response="{\"access_token\":\"test\",\"refresh_token\":\"test\",\"expires_in\":3600,\"user\":{\"id\":\"11111111-1111-4111-8111-111111111111\"}}";
            if(route.get().endsWith("/users"))response="[{\"user_id\":1,\"role\":\"Barangay Official\",\"full_name\":\"Test Official\",\"is_active\":true}]";
            byte[] bytes=response.getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
        });server.start();
        try{
            DataManager data=new DataManager(new SupabaseConfig("http://127.0.0.1:"+server.getAddress().getPort(),"sb_publishable_test"));
            data.login("official@example.test","test-password".toCharArray(),"Barangay Official");
            data.saveSupplyRule("Food","packs",AllocationPlanner.ALL,new BigDecimal("0.125"),3,true);
            check(route.get().endsWith("ems_save_supply_rule")&&body.get().get("p_rate").getAsBigDecimal().equals(new BigDecimal("0.125")),"Decimal rule RPC");
            data.saveCenter(null,"Hall","Zone",10,"Ramp",new String[]{"Wheelchair access"},true);
            check(route.get().endsWith("ems_save_center_profile")&&body.get().get("p_complete").getAsBoolean(),"Facility profile RPC");
            data.register("Person",25,"Family","",1,"","None",false,new String[]{"Accessible toilets"});
            check(route.get().endsWith("ems_register_evacuee_planned")&&body.get().getAsJsonArray("p_requirements").size()==1,"Registration requirements RPC");
            data.updateEvacuee(1,"Person",25,"Family","","","None",new String[0]);
            check(route.get().endsWith("ems_update_evacuee_planned")&&body.get().getAsJsonArray("p_requirements").isEmpty(),"Requirements can be cleared");
            check(data.load().supplyRules.isEmpty(),"New snapshot contract");
            snapshot.set("{\"centers\":[],\"people\":[],\"supplies\":[],\"allocations\":[],\"transfers\":[]}");
            try{data.load();throw new AssertionError("Old snapshot silently accepted");}
            catch(IllegalStateException expected){check(expected.getMessage().contains("10_requirements_planning"),"Migration guidance");}
            System.out.println("PASS: saved-rule precision, profile/registration/edit RPC contracts and missing-migration feedback.");
        }finally{server.stop(0);}
    }
}
