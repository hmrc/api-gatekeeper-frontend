$(document).ready(function() {
  // DataTables
  (function() {
    function getAllFilters (table) {
      return $('*[data-datatable-filter="' + table + '"]');
    };

    function getTableFilters (table) {
      return $('*[data-datatable-filter="' + table + '"]')
               .not('[data-datatable-column-filter]');
    };

    function getCustomFilters (table) {
      return $('*[data-datatable-custom-filter="' + table + '"]');
    }

    function getTableFilterValues (filters) {
      var filterValues = [];

      $.each(filters, function (index, filter) {
        var inputVal;
        inputVal = $(filter).val();

        if (inputVal) {
          filterValues.push(inputVal);
        }
      });

      return filterValues;
    }

    function buildFilter (filterValues) {
      return filterValues.join(' ');
    }

    function searchTable(table) {
      var dataTable = $('#' + table).DataTable();
      var filterValue = buildFilter( getTableFilterValues( getTableFilters(table) ) );

      dataTable.search(filterValue).draw();
    }

    function searchColumn(filter, table, column) {
      var dataTable = $('#' + table).DataTable();
      var filterValue = "";

      if (filter.selectedIndex) {
        filterValue = filter.options[filter.selectedIndex].text;
         if (filterValue) {
          filterValue = '^' + filterValue + '$'; // Filter by exact match
        }
      }

      dataTable
        .columns(column)
        .search(filterValue, true, false)
        .draw();
    }

    function search (e) {
      var table = $(this).data('datatable-filter');
      var column = $(this).data('datatable-column-filter');

      if (column) {
        searchColumn(this, table, parseInt(column));
      } else {
        searchTable(table);
      }
    }

    function dataTableEvents (index, filter) {
      var $filter = $(filter);

      if (filter.options) {
        $filter.on('change', search);
      } else if ($filter.is('input')) {
        $filter.on('keydown', search);
        $filter.on('keypress', search);
        $filter.on('keyup', search);
      }
    }

    function customDraw (e) {
      var table = $(this).data('datatable-custom-filter');
      table = $('#' + table).DataTable();
      table.draw();
    }

    function customEvents (index, filter) {
      var $filter = $(filter);

      $filter.on('keyup', customDraw);
    }

    // Initialize all datatables with filters
    var dataTables = $('table.dataTable');
    $.each(dataTables, function (index, table) {
      var id = $(table).attr('id');
      var filters = getAllFilters(id);

      // trigger table updates on filter changes
      $.each(filters, dataTableEvents);

      // hook up a custom filter triggers
      var customFilters = getCustomFilters(id);
      $.each(customFilters, customEvents);

      // initialize the table
      $(table).DataTable();
    });

    // Register custom search with dataTables
    $.fn.dataTable.ext.search.push(function(settings, data, dataIndex) {
      var table = settings.sTableId;

      var customFilters = getCustomFilters(table);

      var tableFilterValues = getTableFilterValues( customFilters );

      var filterValue = buildFilter( tableFilterValues ).toLowerCase();

      if(table === "applications-table") {
        if (~data[0].toLowerCase().indexOf(filterValue)) return true;
        if (~data[1].toLowerCase().indexOf(filterValue)) return true;
        if (~data[3].toLowerCase().indexOf(filterValue)) return true;
        if (~data[5].toLowerCase().indexOf(filterValue)) return true;
        if (~data[8].toLowerCase().indexOf(filterValue)) return true;
      }
      else {
        if (~data[0].toLowerCase().indexOf(filterValue)) return true;
        if (~data[1].toLowerCase().indexOf(filterValue)) return true;
        if (~data[2].toLowerCase().indexOf(filterValue)) return true;
        if (~data[4].toLowerCase().indexOf(filterValue)) return true;
      }

      return false;
    })

  })();
});
