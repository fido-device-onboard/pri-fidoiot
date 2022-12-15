// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.api;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.fidoalliance.fdo.protocol.Config;
import org.fidoalliance.fdo.protocol.LoggerService;


/**
 * Rest API servlet.
 */
public class RestApiServlet extends HttpServlet {
  protected static final LoggerService logger = new LoggerService(RestApiServlet.class);

  protected static final String API_CLASS = "Api-Class";


  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {

    try (RestApi restApi = (RestApi) Config.loadObject(getInitParameter(API_CLASS))) {
      restApi.init(req, resp);
      restApi.doDelete();
    } catch (NotFoundException e) {
      logger.error("Not found exception ");
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
    } catch (UnsupportedMediaTypeException e) {
      logger.error("Unsupported Media Type Exception");
      resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
    } catch (InternalServerErrorException e) {
      logger.error("Internal Server Error Exception");
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    } catch (BadRequestException e) {
      logger.error("Bad Request Exception");
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    } catch (EntityTooLargeException e) {
      logger.error("Entity is too large");
      resp.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
    } catch (Exception e) {
      logger.error("Internal server error");
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }


  }

  @Override
  protected void doPut(HttpServletRequest req, HttpServletResponse resp) {

    try (RestApi restApi = (RestApi) Config.loadObject(getInitParameter(API_CLASS))) {
      restApi.init(req, resp);
      restApi.doPut();
    } catch (NotFoundException e) {
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
    } catch (UnsupportedMediaTypeException e) {
      logger.error("Unsupported Media Type Exception");
      resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
    } catch (InternalServerErrorException e) {
      logger.error("Internal Server Error ");
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    } catch (BadRequestException e) {
      logger.error("Bad Request Exception ");
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    } catch (EntityTooLargeException e) {
      logger.error("Entity is too large");
      resp.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
    } catch (Exception e) {
      logger.error("Internal server error");
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
    try (RestApi restApi = (RestApi) Config.loadObject(getInitParameter(API_CLASS))) {
      restApi.init(req, resp);
      restApi.doGet();
    } catch (NotFoundException e) {
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
    } catch (UnsupportedMediaTypeException e) {
      logger.error("Unsupported Media Type Exception");
      resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
    } catch (InternalServerErrorException e) {
      logger.error("Internal server error");
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    } catch (BadRequestException e) {
      logger.error("Bad Request error");
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    } catch (EntityTooLargeException e) {
      logger.error("Entity is too large");
      resp.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
    } catch (Exception e) {
      logger.error("Internal server error");
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
    try (RestApi restApi = (RestApi) Config.loadObject(getInitParameter(API_CLASS))) {
      restApi.init(req, resp);
      restApi.doPost();
    } catch (NotFoundException e) {
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
    } catch (UnsupportedMediaTypeException e) {
      logger.error("Unsupported Media Type Error");
      resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
    } catch (InternalServerErrorException e) {
      logger.error("Internal server error");
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    } catch (BadRequestException e) {
      logger.error("Bad Request Error");
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    } catch (EntityTooLargeException e) {
      logger.error("Entity is too large");
      resp.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
    } catch (Exception e) {
      logger.error("Internal server error");
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

  }
}
