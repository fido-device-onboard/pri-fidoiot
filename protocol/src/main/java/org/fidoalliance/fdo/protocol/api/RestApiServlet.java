// Copyright 2022 Intel Corporation
// SPDX-License-Identifier: Apache 2.0

package org.fidoalliance.fdo.protocol.api;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.fidoalliance.fdo.protocol.Config;


/**
 * Rest API servlet.
 */
public class RestApiServlet extends HttpServlet {

  protected static final String API_CLASS = "Api-Class";


  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {

    try (RestApi restApi = (RestApi) Config.loadObject(getInitParameter(API_CLASS))) {
      restApi.init(req, resp);
      restApi.doDelete();
    } catch (NotFoundException e) {
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
    } catch (UnsupportedMediaTypeException e) {
      resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
    } catch (InternalServerErrorException e) {
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    } catch (BadRequestException e) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    } catch (EntityTooLargeException e) {
      resp.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
    } catch (Exception e) {
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
      resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
    } catch (InternalServerErrorException e) {
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    } catch (BadRequestException e) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    } catch (EntityTooLargeException e) {
      resp.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
    } catch (Exception e) {
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
      resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
    } catch (InternalServerErrorException e) {
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    } catch (BadRequestException e) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    } catch (EntityTooLargeException e) {
      resp.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
    } catch (Exception e) {
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
      resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
    } catch (InternalServerErrorException e) {
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    } catch (BadRequestException e) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    } catch (EntityTooLargeException e) {
      resp.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
    } catch (Exception e) {
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

  }
}
