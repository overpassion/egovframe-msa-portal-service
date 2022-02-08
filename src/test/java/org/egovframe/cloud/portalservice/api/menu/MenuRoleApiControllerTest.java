package org.egovframe.cloud.portalservice.api.menu;

import lombok.extern.slf4j.Slf4j;
import org.egovframe.cloud.portalservice.api.menu.dto.*;
import org.egovframe.cloud.portalservice.domain.menu.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Slf4j
@SpringBootTest(webEnvironment = RANDOM_PORT)
class MenuRoleApiControllerTest {


    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private MenuRoleRepository menuRoleRepository;

    @BeforeEach
    public void setup() throws Exception {
        Site site = Site.builder()
                .name("site")
                .isUse(true)
                .build();
        siteRepository.save(site);

        Menu parentMenu = menuRepository.save(Menu.builder()
                .menuKorName("parent")
                .sortSeq(1)
                .site(site)
                .build());

        for (int i = 0; i < 3; i++) {
            Menu childMenu = Menu.builder()
                    .menuKorName("child_" + i)
                    .site(site)
                    .parent(parentMenu)
                    .sortSeq(i + 1)
                    .build();
            childMenu.setParentMenu(parentMenu);
            menuRepository.save(childMenu);
        }
    }

    @AfterEach
    public void cleanup() throws Exception {
        menuRoleRepository.deleteAll();
        menuRepository.deleteAll();
        siteRepository.deleteAll();
    }

    @Test
    public void 권한별메뉴_데이터없이_메뉴outerjoin_하여_조회한다() throws Exception {
        Site site = siteRepository.findAll().get(0);
        //when
        ResponseEntity<List<MenuRoleResponseDto>> responseEntity =
                restTemplate.exchange("/api/v1/menu-roles/role/"+site.getId(), HttpMethod.GET, null, new ParameterizedTypeReference<List<MenuRoleResponseDto>>(){});

        //then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<MenuRoleResponseDto> body = responseEntity.getBody();
        assertThat(body.size()).isEqualTo(1);
        assertThat(body.get(0).getChildren().size()).isEqualTo(3);
        body.stream().forEach(menuTreeResponseDto -> {
            System.out.println(menuTreeResponseDto.toString());
            menuTreeResponseDto.getChildren().stream().forEach(System.out::println);
        });
    }

    @Test
    public void 권한별메뉴_데이터있는경우_조회한다() throws Exception {

        List<Menu> menus = menuRepository.findAll();
        Menu parent = menus.stream().filter(menu -> menu.getMenuKorName().equals("parent")).collect(Collectors.toList()).get(0);
        Menu child1 = menus.stream().filter(menu -> menu.getMenuKorName().equals("child_1")).collect(Collectors.toList()).get(0);

        List<MenuRole> menuRoles = new ArrayList<>();
        menuRoles.add(MenuRole.builder().roleId("role").menu(parent).build());
        menuRoles.add(MenuRole.builder().roleId("role").menu(child1).build());
        menuRoleRepository.saveAll(menuRoles);

        Site site = siteRepository.findAll().get(0);
        //when
        ResponseEntity<List<MenuRoleResponseDto>> responseEntity =
                restTemplate.exchange("/api/v1/menu-roles/role/"+site.getId(), HttpMethod.GET, null, new ParameterizedTypeReference<List<MenuRoleResponseDto>>(){});


        //then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<MenuRoleResponseDto> body = responseEntity.getBody();
        assertThat(body.size()).isEqualTo(1);
        assertThat(body.get(0).getIsChecked()).isTrue();
        body.stream().forEach(System.out::println);
        assertThat(body.get(0).getChildren().size()).isEqualTo(3);
        body.stream().forEach(menuTreeResponseDto -> {
            menuTreeResponseDto.getChildren().stream().forEach(child -> {
                System.out.println(child);
                if (child.getKorName().equals("child_1")) {
                    assertThat(child.getIsChecked()).isTrue();
                }else {
                    assertThat(child.getIsChecked()).isFalse();
                }
            });

        });

    }

    @Test
    public void 권한별메뉴관리_저장한다() throws Exception {
        Site site = siteRepository.findAll().get(0);
        List<MenuRoleResponseDto> list = menuRoleRepository.findTree("role", site.getId());

        List<MenuRoleRequestDto> requestDtoList = new ArrayList<>();
        List<MenuRoleRequestDto> children = new ArrayList<>();
        list.get(0).getChildren().stream().forEach(menuRoleResponseDto -> {
            if (menuRoleResponseDto.getKorName().equals("child_1")) {
                children.add(MenuRoleRequestDto.builder()
                        .menuRoleId(menuRoleResponseDto.getMenuRoleId())
                        .isChecked(true)
                        .roleId("role")
                        .id(menuRoleResponseDto.getId())
                        .build());

            }else {
                children.add(MenuRoleRequestDto.builder()
                        .menuRoleId(menuRoleResponseDto.getMenuRoleId())
                        .isChecked(false)
                        .roleId("role")
                        .id(menuRoleResponseDto.getId())
                        .build());
            }
        });

        requestDtoList.add(MenuRoleRequestDto.builder()
                .menuRoleId(list.get(0).getMenuRoleId())
                .isChecked(true)
                .id(list.get(0).getId())
                .children(children)
                .build());

        HttpEntity<List<MenuRoleRequestDto>> httpEntity = new HttpEntity<>(
                requestDtoList
        );



        //when
        ResponseEntity<String> responseEntity =
                restTemplate.exchange("/api/v1/menu-roles", HttpMethod.POST, httpEntity, String.class);

        //then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isEqualTo("Success");

        List<MenuRole> roles = menuRoleRepository.findAll();
        roles.stream().forEach(System.out::println);
        assertThat(roles.size()).isEqualTo(2);

    }

    @Test
    public void 로그인하지_않은_사용자의_메뉴조회() throws Exception {
        //given
        Site site = siteRepository.findAll().get(0);
        Menu parentMenu = menuRepository.save(Menu.builder()
                .menuKorName("parent-any")
                .sortSeq(1)
                .site(site)
                .build());
        MenuRole parentMenuRole = MenuRole.builder()
                .roleId("ROLE_ANONYMOUS")
                .menu(parentMenu)
                .build();
        parentMenuRole.setMenu(parentMenu);
        menuRoleRepository.save(parentMenuRole);

        for (int i = 0; i < 3; i++) {
            Menu childMenu = Menu.builder()
                    .menuKorName("child-any_" + i)
                    .site(site)
                    .parent(parentMenu)
                    .sortSeq(i + 1)
                    .build();
            childMenu.setParentMenu(parentMenu);
            menuRepository.save(childMenu);
            MenuRole role_any = MenuRole.builder()
                    .roleId("ROLE_ANONYMOUS")
                    .menu(childMenu)
                    .build();
            role_any.setMenu(childMenu);
            menuRoleRepository.save(role_any);
        }
        //when
        ResponseEntity<List<MenuSideResponseDto>> responseEntity =
                restTemplate.exchange("/api/v1/menu-roles/"+site.getId(), HttpMethod.GET, null, new ParameterizedTypeReference<List<MenuSideResponseDto>>(){});


        //then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<MenuSideResponseDto> body = responseEntity.getBody();
        assertThat(body.size()).isEqualTo(1);
        body.stream().forEach(menuSideResponseDto -> {
            System.out.println(menuSideResponseDto);
            menuSideResponseDto.getChildren().stream().forEach(System.out::println);
        });

    }
}