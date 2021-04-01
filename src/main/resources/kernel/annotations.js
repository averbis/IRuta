define(["jquery"], function ($) {
  "use strict";

  var AnnotationViewer = function (element) {
    this.colorIndex = [];
    this.checkboxes = null;
    this.element = element;
    this.init();
  };

  AnnotationViewer.prototype = {
    init: function () {
      console.log("Creating annotation viewer");

      var that = this;

      this.checkboxes = $(this.element).find(".controls input");
      this.checkboxes.change((event) => that.toggleAnnotation(event.target));

      $(this.element)
        .find(".on")
        .click(() => that.allOn());
      $(this.element)
        .find(".off")
        .click(() => that.allOff());

      $(this.element)
        .find(".text b")
        .each((i, character) => {
          character = $(character);
          character.click((event) => that.toggleDetails(event.target));
          character.mouseenter((event) => that.showInfo(event.target));
          character.mouseleave((event) => that.removeInfo(event.target));
        });

      $(this.element)
        .find(".annotations pre")
        .each((i, detail) => {
          detail = $(detail);
          detail.mouseenter((event) => that.showDetailInfo(event.target));
          detail.mouseenter((event) => that.removeDetailInfo(event.target));
        });

      $(this.element).find(".cas").removeClass("hidden");
    },

    allOn: function () {
      $(this.element).find(".cas").addClass("hidden");

      this.checkboxes.each((i, checkbox) => {
        checkbox = $(checkbox);
        if (!checkbox.prop("checked")) checkbox.click();
      });

      $(this.element).find(".cas").removeClass("hidden");
    },

    allOff: function () {
      $(this.element).find(".cas").addClass("hidden");

      this.checkboxes.each((i, checkbox) => {
        checkbox = $(checkbox);
        if (checkbox.prop("checked")) {
          checkbox.prop("checked", false);
        }
        checkbox.parent().removeClass("underline");
      });

      // Remove all classes except the "annotations"/"text" classes (the other classes control)
      // which annotation types are shown
      $(this.element).find(".text").attr("class", "text");
      $(this.element).find(".annotations").attr("class", "annotations");

      var pre = $(this.element).find(".annotations .inline-block");
      for (var i = pre.length - 1; i >= 0; i--) {
        pre[i].classList.remove("inline-block");
      }

      this.colorIndex = [];

      $(this.element).find(".cas").removeClass("hidden");
    },

    toggleAnnotation: function (checkbox) {
      checkbox = $(checkbox);
      var type = checkbox.prop("value");

      if (checkbox.prop("checked")) {
        $(this.element).find(".annotations").addClass(type);
        this.setColor(type, $(this.element).find(".text"));
        this.setUnderline(checkbox.parent());
      } else {
        $(this.element).find(".annotations").removeClass(type);
        this.unsetColor(type);
        this.unsetUnderline(checkbox.parent());
      }
    },

    setColor: function (type, element) {
      var index = this.colorIndex.push(type);
      $(this.element)
        .find(".text")
        .addClass(this.getPostfixedType(type, index - 1));
    },

    unsetColor: function (type, element) {
      var index = this.colorIndex.indexOf(type);
      this.colorIndex.splice(index, 1);
      var textElement = $(this.element).find(".text");
      textElement.attr("class", "text");
      $.each(this.colorIndex, (i) =>
        textElement.addClass(this.getPostfixedType(this.colorIndex[i], i))
      );
    },

    setUnderline: function (label) {
      this.checkboxes.parent().removeClass("underline");

      $(label).addClass("underline");
    },

    unsetUnderline: function (label) {
      if (!label.hasClass("underline")) return;

      label.removeClass("underline");

      if (this.colorIndex.length > 0) {
        var type = this.colorIndex[this.colorIndex.length - 1];
        $(this.element).find(`.controls .${type}`).removeClass("underline");
      }
    },

    getPostfixedType: function (type, index) {
      if (index > 0) {
        return type + "-" + index;
      } else {
        return type;
      }
    },

    toggleDetails: function (character) {
      $(this.element)
        .find(".annotations .inline-block")
        .removeClass("inline-block");

      for (var i = 0; i < character.classList.length; i++) {
        if (character.classList[i].lastIndexOf("a", 0) === 0) {
          $(this.element)
            .find(`[data-id='${character.classList[i]}']`)
            .addClass("inline-block");
        }
      }
    },

    showInfo: function (character) {
      if (window.getComputedStyle(character).backgroundColor == "transparent")
        return;
      var annotations = this.getTopAnnotations(character);
      for (var i = 0; i < annotations.length; i++) {
        this.showDetailInfo(annotations[i]);
      }
    },

    removeInfo: function (character) {
      if (window.getComputedStyle(character).backgroundColor == "transparent")
        return;
      var annotations = this.getTopAnnotations(character);
      for (var i = 0; i < annotations.length; i++) {
        this.removeDetailInfo(annotations[i]);
      }
    },

    showDetailInfo: function (detail) {
      detail = $(detail);
      var characters = $(this.element)
        .find(`.text .${detail.data("id")}`)
        .addClass("inline-block emph");
    },

    removeDetailInfo: function (detail) {
      detail = $(detail);
      var characters = $(this.element)
        .find(`.text .${detail.data("id")}`)
        .removeClass("inline-block emph");
    },

    getTopType: function (character) {
      for (var i = this.colorIndex.length - 1; i >= 0; i--) {
        if (character.classList.contains(this.colorIndex[i])) {
          return this.colorIndex[i];
        }
      }
      return null;
    },

    getTopAnnotations: function (character) {
      var type = this.getTopType(character);
      var annotations = [];
      for (var i = 0; i < character.classList.length; i++) {
        if (!(character.classList[i].lastIndexOf("a", 0) === 0)) continue;
        $(this.element)
          .find(`.annotations [data-id='${character.classList[i]}'].${type}`)
          .each((i, ann) => annotations.push(ann));
      }
      return annotations;
    },
  };

  var onload = function () {
    $(".annotation-viewer").each(function () {
      if (!$(this).data("annotation-viewer")) {
        $(this).data("annotation-viewer", true);
        new AnnotationViewer(this);
      }
    });
  };

  return {
    onload: onload,
  };
});
