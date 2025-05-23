package com.yelman.cloudserver.services;

import com.itextpdf.text.DocumentException;
import com.yelman.cloudserver.api.dto.AnimalHealthDto;
import com.yelman.cloudserver.api.dto.PdfSensorDto;
import com.yelman.cloudserver.api.dto.SensorDto;
import com.yelman.cloudserver.model.*;
import com.yelman.cloudserver.repository.*;
import com.yelman.cloudserver.services.core.FuzzyLogicService;
import com.yelman.cloudserver.services.impl.AnimalHealthRuntimeImpl;
import com.yelman.cloudserver.utils.mail.EmailNotificationService;
import com.yelman.cloudserver.utils.pdf.PdfGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Service
public class AnimalHealthRuntimeServices implements AnimalHealthRuntimeImpl {

    private final ChewingActivityRepository chewingActivityRepository;
    private final HeartBeatRepository heartBeatRepository;
    private final AnimalRepository animalRepository;
    private final TemperatureHumidityRepository temperatureHumidityRepositoryRepository;
    private final FuzzyLogicService fuzzyLogicService;
    private final PdfGenerator pdfGenerator;
    private final CompanyRepository companyRepository;
    private final EmailNotificationService emailService;


    public AnimalHealthRuntimeServices(ChewingActivityRepository chewingActivityRepository,
                                       HeartBeatRepository heartBeatRepository,
                                       AnimalRepository animalRepository,
                                       TemperatureHumidityRepository temperatureHumidityRepositoryRepository,
                                       FuzzyLogicService fuzzyLogicService,
                                       PdfGenerator pdfGenerator,
                                       CompanyRepository companyRepository,
                                       EmailNotificationService emailService) {
        this.chewingActivityRepository = chewingActivityRepository;
        this.heartBeatRepository = heartBeatRepository;
        this.animalRepository = animalRepository;
        this.temperatureHumidityRepositoryRepository = temperatureHumidityRepositoryRepository;
        this.fuzzyLogicService = fuzzyLogicService;
        this.pdfGenerator = pdfGenerator;
        this.companyRepository = companyRepository;
        this.emailService = emailService;

    }

    @Override
    public AnimalHealthDto getAnimals(String animalTagId, Pageable pageable) {

        Page<HeartBeat> h = heartBeatRepository.findByAnimal_TagId(animalTagId, pageable);
        Page<TemperatureHumidity> a = temperatureHumidityRepositoryRepository.findByAnimal_TagId(animalTagId, pageable);
        Page<ChewingActivity> c = chewingActivityRepository.findByAnimal_TagId(animalTagId, pageable);

        AnimalHealthDto animalHealthDto = new AnimalHealthDto();
        animalHealthDto.setHeartBeats(h.getContent());
        animalHealthDto.setTemperatureHumidities(a.getContent());
        animalHealthDto.setChewingActivities(c.getContent());

        animalHealthDto.setCurrentPage(h.getNumber());
        animalHealthDto.setTotalPages(h.getTotalPages());
        animalHealthDto.setTotalElements(h.getTotalElements());
        return animalHealthDto;

    }

    @Transactional
    @Override
    public boolean addAnimalHealthHourlyRuntime(SensorDto animal) {
        boolean a = addChewingActivity(animal.getAnimalId(), animal.getChewingActivity());
        boolean b = addHeartBeat(animal.getAnimalId(), animal.getHeartBeat());
        boolean c = addTemperatureHumidity(animal.getAnimalId(), animal.getTemperature(), animal.getHumidity());
        if (a && b && c) {
            fuzzyLogicService.fuzzyLogicRealtime(animal);
            return true;
        }
        return false;
    }

    public void dailyFuzzyLogic(Long companyId) throws IOException {
        Company company = companyRepository.findById(companyId).orElseThrow();
        List<Long> animalId = animalRepository.findAllAnimalIdByCompany_Id(companyId);
        for (Long animalId1 : animalId) {
            fuzzyLogicService.dailyFuzzyLogic(animalId1);
        }
    }

    @Override
    @Transactional
    public void weeklyAndDailyPdfMailLogic(Long companyId, boolean weekIsDay) throws DocumentException, IOException {
        Company company = companyRepository.findById(companyId).orElseThrow();
        List<Long> animalId = animalRepository.findAllAnimalIdByCompany_Id(companyId);
        List<PdfSensorDto> dto = new ArrayList<>();
        if (weekIsDay) {
            for (Long animalId1 : animalId) {
                dto.add(fuzzyLogicService.weeklyPdfMailLogic(animalId1));
            }
        } else {
            for (Long animalId1 : animalId) {
                dto.add(fuzzyLogicService.dailyPdfMailLogic(animalId1));
            }
        }
        byte[] pdf = pdfGenerator.export(dto);
        emailService.sendEmailAtc(pdf, company.getUser().getEmail());
    }

    @Transactional
    @Override
    public void deleteAllSensorAnimalId(Long animalId) {
        deleteChewingActivity(animalId);
        deleteHeartBeat(animalId);
        deleteTemperatureAndHumidity(animalId);
    }

    private void deleteChewingActivity(Long animalId) {
        getByAnimal(animalId).ifPresent(chewingActivity -> {
            chewingActivityRepository.deleteById(chewingActivity.getId());
        });
    }

    private void deleteTemperatureAndHumidity(Long animalId) {
        getByAnimal(animalId).ifPresent(chewingActivity -> {
            temperatureHumidityRepositoryRepository.deleteById(chewingActivity.getId());
        });

    }


    private void deleteHeartBeat(Long animalId) {
        getByAnimal(animalId).ifPresent(chewingActivity -> {
            heartBeatRepository.deleteById(chewingActivity.getId());
        });

    }


    private boolean addChewingActivity(Long animalId, int chewCount) {
        ChewingActivity chewingActivity = new ChewingActivity();
        chewingActivity.setChewCount(chewCount);
        chewingActivity.setAnimal(getByAnimal(animalId).get());
        if (chewingActivity.getAnimal() == null) return false;

        chewingActivityRepository.save(chewingActivity);
        return true;
    }

    private boolean addHeartBeat(Long animalId, int heartBeatCount) {
        HeartBeat heartBeat = new HeartBeat();
        heartBeat.setHeartBeatRate(heartBeatCount);
        heartBeat.setAnimal(getByAnimal(animalId).get());
        if (heartBeat.getAnimal() == null) return false;
        heartBeatRepository.save(heartBeat);
        return true;
    }

    private boolean addTemperatureHumidity(Long animalId, double temperature, double humidity) {
        TemperatureHumidity temperatureHumidity = new TemperatureHumidity();
        temperatureHumidity.setHumidity(humidity);
        temperatureHumidity.setAnimal(getByAnimal(animalId).get());
        if (temperatureHumidity.getAnimal() == null) return false;
        temperatureHumidity.setTemperature(temperature);
        temperatureHumidityRepositoryRepository.save(temperatureHumidity);
        return true;
    }

    private Optional<Animal> getByAnimal(Long animalId) {
        Optional<Animal> animal = animalRepository.findById(animalId);
        if (animal.isPresent()) {
            return animal;
        }
        return null;
    }
}
