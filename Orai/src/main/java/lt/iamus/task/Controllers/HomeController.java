package lt.iamus.task.Controllers;

import java.sql.Timestamp;
import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import lt.iamus.task.Services.BuildingService;
import lt.iamus.task.Services.DevicelogService;
import lt.iamus.task.Services.EnergyConsumedService;
import lt.iamus.task.Services.WeatherService;
import lt.iamus.task.entity.Devicelog;
import lt.iamus.task.entity.EnergyConsumed;
import lt.iamus.task.entity.Weather;

@Controller
public class HomeController {
	
	@Autowired
	private WeatherService weatherService;
	@Autowired
	private DevicelogService devicelogService;
	@Autowired
	private BuildingService buildingService;
	@Autowired
	private EnergyConsumedService energyConsumedService;
	
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String home(Model model) {
		
//		Timer timer = new Timer();
//		/*
//		 * Paleidzia funkcija kas 24 val generuoti energy_consumed duomenu baze. Is siu duomenu atvaizduojamos
//		 * diagramos kiekvieno irenginio. 
//		 */
//		timer.schedule(new TimerTask() {
//		    @Override
//		    public void run() {
//		    	Calendar cal = Calendar.getInstance();
//		    	int currentYear = cal.get(Calendar.YEAR);
//		    	int monthOfYear = cal.get(Calendar.MONTH)+1;
//		    	int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
//
//		       /*
//		        * Kai paleidziamas serveris sugeneruojami energy_consumed lenteles irasai. Generuojama nuo praeitos dienos 
//		        * (nes turim jau duomenis) iki siandienos. Ir kartojamas kas 24 val.
//		        */
//				List<Devicelog> devicelogsList = devicelogService.getTenantList();
//		 		for (int i=0; i<devicelogsList.size(); i++) {
//		 			System.out.println("size " +devicelogsList.size());
//		 			System.out.println("i "+i+" "+devicelogsList.get(i).getTenant());
//				generatorEngConmd(devicelogsList.get(i).getTenant(), currentYear, monthOfYear, dayOfMonth-1, dayOfMonth);
//		 		}
//		    }
//		 }, 0, 1000 * 60 * 60 * 24);
		return "home";
	}
	
	/*
	 * Metodas paima duomenis is Building lenteles, suskaiciuoja kiekvieno pastato silumos laidumo
	 * koeficienta. Paima duomenis is Weather lenteles pagal miesta ir data. Paima duomenis is DeviceLog lenteles
	 * ir suskaiciuoja skirtuma tarp laiko temperaturos ir pastato vidaus temperaturos. Jeigu pagal tenantName
	 *  ir data nerandamas irasas energy_consumed lenteleje, irasas irasomas i lentele. Tokiu budu sugeneruojami 
	 *  energy_consumed lenteles duomenys.
	 */
 	public void generatorEngConmd (String tenantName, int metai, int menesis, int gnrNuoDienos, int gnrIkiDienos) {
 		
 		Random random = new Random();
	    List<Devicelog> devicelogsList = devicelogService.findTenantInfo(tenantName);
	    boolean findValue = tenantName.indexOf("KLAIPEDA") >=0;
	    String city;
	    if(findValue) {
	    	city = "Klaipeda";
	    } else city = "Vilnius";
	    
	    Integer buildingId = devicelogsList.get(0).getBuildingId();
		int year = metai;
		int month = menesis;
		int day = gnrNuoDienos;
		int hour = 0;
		String dataSet = " ";
		
		for (int i=0; i>-1; i++) {
			System.out.println(tenantName);
			int tenantId = devicelogsList.get(i).getId();
		try {	
			String deviceData = "";
	
			if (hour>=24) {hour=0; day++;}
	
			switch (month) {
				case 1: case 3: case 5: case 7: case 8: case 10: 
				if (day>=32) {day=1; month++;}	
					break;
				case 2 :
					if(day>=29) {day=1; month++;}
					break;
				case 4: case 6: case 9: case 11:
					if (day>=31) {day=1; month++;}
					break;
				case 12:
					if (day>=32) {day=1; month=1; year++;}
					break;
			}
			
			String hourStr = ""+hour;
			String monthStr = ""+month;
			String weatherMonth = month +11+"";
			String dayStr = ""+day;
			if (hour<10) {
				hourStr = String.format("%02d", hour);
			}
			if (day<10) {
				dayStr = String.format("%02d", day);
			}
			if (month<10) {
				monthStr = String.format("%02d", month);
			}
						
			deviceData = ""+year+"-"+monthStr+"-"+dayStr+" "+hourStr+"%";
			String weatherData = ""+year+"-"+weatherMonth+"-"+dayStr+" "+hourStr+"%";
			dataSet = ""+year+"-"+weatherMonth+"-"+dayStr+" "+hourStr+":01:00";
			
			double deviceTemp = devicelogService.getAvgTemp(tenantName, deviceData);
			double weatherTemp = weatherService.getTemp(city, weatherData);
			double buildingKof = buildingService.getKof(buildingId);
			System.out.println("suveike TRY "+dataSet);
			System.out.println("deviceTemp "+ deviceTemp);
			System.out.println("weatherTemp "+ weatherTemp);
			System.out.println("buildingKof "+buildingKof); 
	
			 double energyKwh = energyPerHour(buildingKof, weatherTemp, deviceTemp);
			 System.out.println("energyKwh "+energyKwh);
			 try {
				 /*
				  * Jei nuluzta, vadinasi nera iraso duomenu bazeje
				  */
				EnergyConsumed engCnsmd = energyConsumedService.getEngCnsmdToChart(tenantName, weatherData);
			} catch (Exception e) {
				double randomEnergy = random.nextDouble() * 10;
				System.out.println("suveike CATCH del energyConsumed");
				EnergyConsumed energyConsumed = new EnergyConsumed();
				energyConsumed.setBuildingId(buildingId);
				energyConsumed.setTenantId(tenantId);
				energyConsumed.setTenantName(tenantName);
				energyConsumed.setDate(Timestamp.valueOf(dataSet));
				energyConsumed.setEnergyKwh(energyKwh);
				/*
				 * Atsitiktine tvarka generuojama suvartota energija pastato silumai palaikyti. Irasai su lyginiu
				 * building_id suvartoja maziau silumos nei rekomenduojama, nei turetu suvartoti. Irasai su nelyginiu
				 * building_id suvartoja daugiau nei turetu.
				 */
				if (buildingId%2==0) {
					double usedEnergy = energyKwh - randomEnergy;
					energyConsumed.setUsedEnergy(usedEnergy);
				} else {energyConsumed.setUsedEnergy(energyKwh+randomEnergy);}			
				energyConsumedService.addEnergyConsumed(energyConsumed);
			}
		} catch (Exception e) {
			System.out.println("suveike CATCH del TEMP ar KOF");
			System.out.println("data "+dataSet);
		}
		hour++;
		if(day>=gnrIkiDienos) {
			System.out.println("suveike return");
			return;
		}
		}
 	}
	
	/*
	 * Kadangi serveris yra lokalus, (nakti kompas isjungtas) tai atsiranda "tarpai" tarp irasu iterpimu.
	 * Kad nebutu dideliu tarpu tarp irasu, metodas paima tos dienos lista (dienos, kurioje truksta irasu kas
	 * valanda) ir atstiktine tvarka iterpia i tuscias vietas irasa. 
	 */
 	public void generatorWeather (String city) {
		int year = 2018;
		int month = 12;
		int day = 4;
		int hour = 7;
		String dataSet = "";	
		String dayDate = "";
		for (int i=1; i>0; i++) {			
		try {	
		String date = "";
			if (hour>=24) {hour=0; day++;}
			switch (month) {
				case 1: case 3: case 5: case 7: case 8: case 10: 
				if (day>=32) {day=1; month++;}	
					break;
				case 2 :
					if(day>=29) {day=1; month++;}
					break;
				case 4: case 6: case 9: case 11:
					if (day>=31) {day=1; month++;}
					break;
				case 12:
					if (day>=32) {day=1; month=1; year++;}
					break;
			}
			String hourStr = ""+hour;
			String monthStr = ""+month;
			String dayStr = ""+day;
			if (hour<10) {
				hourStr = String.format("%02d", hour);
			}
			if (day<10) {
				dayStr = String.format("%02d", day);
			}
			if (month<10) {
				monthStr = String.format("%02d", month);
			}
						
			date = ""+year+"-"+monthStr+"-"+dayStr+" "+hourStr+"%";
			dataSet = ""+year+"-"+monthStr+"-"+dayStr+" "+hourStr+":01:00";
			dayDate = ""+year+"-"+monthStr+"-"+dayStr+"%";
			
			Double verte = weatherService.getTemp(city, date);
			System.out.println("suveike TRY "+dataSet);
		
		} catch (Exception e) {
			System.out.println("suveike CATCH");
			System.out.println(dayDate);
			List<Weather> weathersList = weatherService.getWeatherListByDate(city, dayDate);
			Random random = new Random();
			int x = random.nextInt(weathersList.size());
			Weather weather = weathersList.get(x);
			weather.setId(null);
			weather.setTstamp(dataSet);
			weatherService.addWeatherInfo(weather);
		}
		if(dataSet.equals("2018-12-11 00:01:00")) {
			System.out.println("suveike return");
			return;
		}
		hour++;
		}
	}
	/*
	 * Skaiciuoja energijos suvartojima, tai rekomendacija kiek energijos turejo buti isleista pastatui 
	 * sildyti. Formule paimta is interneto
	 */
	public double energyPerHour (double buildingKof, double weatherTemp, double deviceTemp) {
		double energyOneHour = (weatherTemp - deviceTemp)*(-1) * buildingKof;
		return energyOneHour;
	}
	
}
